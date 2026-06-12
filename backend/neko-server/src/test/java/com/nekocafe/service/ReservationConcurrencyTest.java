package com.nekocafe.service;

import com.nekocafe.common.ApiException;
import com.nekocafe.entity.Reservation;
import com.nekocafe.entity.User;
import com.nekocafe.mapper.ReservationEventMapper;
import com.nekocafe.mapper.ReservationMapper;
import com.nekocafe.mapper.UserMapper;
import com.nekocafe.security.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * FR-TABLE-002 集成式并发测试（服务层 + 内存版数据库语义）。
 *
 * <p>用 ConcurrentHashMap.putIfAbsent 精确模拟 MySQL 生成列唯一约束
 * {@code uq_reservations_active_slot} 的原子拒绝语义（created/booked/seated/dining 占用时段、
 * cancelled/finished/no_show 置 NULL 释放），覆盖三类验收场景：
 * <ol>
 *   <li>两个并发请求同店同桌同日同时段：恰好 1 条成功、1 条 409、库中仅 1 条有效预约；</li>
 *   <li>顺序重复创建：第二次被时段预检拦截返回 409；</li>
 *   <li>booked → cancelled 释放时段后，同桌位同时段可再次预约成功。</li>
 * </ol>
 */
class ReservationConcurrencyTest {

    private static final long TABLE_ID = 5L;
    private static final long STORE_ID = 1L;
    private static final String SLOT_TAKEN_MESSAGE = "该桌位在该时段已被预约，请重新选择桌位或时间";

    private ReservationMapper reservationMapper;
    private UserMapper userMapper;
    private ReservationService service;

    /** 内存版唯一索引：active_slot_key -> reservation id，putIfAbsent 即数据库的原子唯一性检查。 */
    private final ConcurrentHashMap<String, Long> activeSlotIndex = new ConcurrentHashMap<>();
    /** 内存版 reservations 表。 */
    private final ConcurrentHashMap<Long, Reservation> rows = new ConcurrentHashMap<>();
    private final AtomicLong reservationIds = new AtomicLong();
    private final AtomicLong userIds = new AtomicLong(9);
    /** cancel 流程先 selectForUpdate 再 update，记录被锁行以便 update 作用到正确的行。 */
    private final AtomicLong lockedReservationId = new AtomicLong();

    @BeforeEach
    void setup() {
        reservationMapper = mock(ReservationMapper.class);
        ReservationEventMapper reservationEventMapper = mock(ReservationEventMapper.class);
        userMapper = mock(UserMapper.class);
        UserService userService = mock(UserService.class);
        service = new ReservationService(reservationMapper, reservationEventMapper, userMapper, userService);

        // 每次建档返回新顾客（两个并发请求来自不同顾客，对应验收场景）
        when(userMapper.selectOne(any())).thenAnswer(inv -> {
            User user = new User();
            user.setId(userIds.getAndIncrement());
            user.setRole("customer");
            return user;
        });

        // 手动选桌：桌位行始终存在、门店一致、容量足够、状态 available
        when(reservationMapper.lockTableById(TABLE_ID)).thenAnswer(inv -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", TABLE_ID);
            row.put("store_id", STORE_ID);
            row.put("code", "A01");
            row.put("seats", 4);
            row.put("status", "available");
            return row;
        });

        // 时段预检：查内存版唯一索引（并发测试中会按需覆盖为“双方都看到空时段”）
        when(reservationMapper.countActiveOnSlot(anyLong(), anyString(), anyString())).thenAnswer(inv ->
                activeSlotIndex.containsKey(slotKey(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)))
                        ? 1 : 0);

        // 插入：putIfAbsent 原子抢占时段，失败抛 DuplicateKeyException —— 与数据库唯一约束行为一致
        when(reservationMapper.insert(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setId(reservationIds.incrementAndGet());
            String key = slotKey(r.getTableId(), r.getReservationDate(), r.getReservationTime());
            if (activeSlotIndex.putIfAbsent(key, r.getId()) != null) {
                throw new DuplicateKeyException(
                        "Duplicate entry '" + key + "' for key 'reservations.uq_reservations_active_slot'");
            }
            rows.put(r.getId(), r);
            return 1;
        });

        when(reservationMapper.selectForUpdate(anyLong())).thenAnswer(inv -> {
            long id = inv.getArgument(0);
            lockedReservationId.set(id);
            return rows.get(id);
        });

        // 状态更新（本测试仅 cancel 触发）：模拟生成列行为 —— 离开活跃状态集即释放时段
        when(reservationMapper.update(isNull(), any())).thenAnswer(inv -> {
            Reservation row = rows.get(lockedReservationId.get());
            row.setStatus("cancelled");
            activeSlotIndex.values().removeIf(id -> id.equals(row.getId()));
            return 1;
        });

        when(reservationMapper.getReservationDetail(anyLong())).thenAnswer(inv -> {
            Reservation row = rows.get((Long) inv.getArgument(0));
            if (row == null) {
                return null;
            }
            Map<String, Object> detail = new HashMap<>();
            detail.put("id", row.getId());
            detail.put("status", row.getStatus());
            return detail;
        });
    }

    private static String slotKey(Object tableId, Object date, Object time) {
        return tableId + "#" + date + "#" + time;
    }

    private static com.nekocafe.dto.ReservationRequest request(String mobile) {
        String tomorrow = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1).toString();
        return new com.nekocafe.dto.ReservationRequest("测试顾客", mobile, STORE_ID, TABLE_ID, null,
                tomorrow, "18:30", 2, null, null);
    }

    private long countActiveRows() {
        Set<String> active = Set.of("created", "booked", "seated", "dining");
        return rows.values().stream().filter(r -> active.contains(r.getStatus())).count();
    }

    @Test
    @DisplayName("FR-TABLE-002 验收：并发抢同一桌位同一时段，仅 1 成功、其余 409、库中仅 1 条有效预约")
    void concurrentCreateOnSameSlotAllowsExactlyOne() throws Exception {
        // 两个“事务”都完成时段预检后才放行插入，模拟一致性读快照都看到空时段的最坏情况，
        // 此时业务预检全部通过，只能依赖唯一约束兜底
        CyclicBarrier bothPassedChecks = new CyclicBarrier(2);
        when(reservationMapper.countActiveOnSlot(anyLong(), anyString(), anyString())).thenAnswer(inv -> {
            bothPassedChecks.await(5, TimeUnit.SECONDS);
            return 0;
        });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<String>> outcomes;
        try {
            Callable<String> first = () -> tryCreate("13800000111");
            Callable<String> second = () -> tryCreate("13800000222");
            outcomes = pool.invokeAll(List.of(first, second));
        } finally {
            pool.shutdownNow();
        }

        int success = 0;
        int conflict = 0;
        for (Future<String> outcome : outcomes) {
            String result = outcome.get();
            if (result.equals("success")) {
                success++;
            } else if (result.equals("409:" + SLOT_TAKEN_MESSAGE)) {
                conflict++;
            } else {
                throw new AssertionError("预期 success 或 409 + 验收文案，实际: " + result);
            }
        }

        assertEquals(1, success, "并发创建同一桌位同一时段应恰好 1 条成功");
        assertEquals(1, conflict, "失败请求应返回 409 且文案符合验收口径");
        assertEquals(1, activeSlotIndex.size(), "唯一索引中该时段应只有一条占用");
        assertEquals(1, countActiveRows(), "数据库中不得出现重复有效预约");
    }

    private String tryCreate(String mobile) {
        try {
            Map<String, Object> detail = service.create(request(mobile));
            assertNotNull(detail.get("id"));
            return "success";
        } catch (ApiException ex) {
            return ex.getStatus() + ":" + ex.getMessage();
        }
    }

    @Test
    @DisplayName("顺序重复创建同一桌位同一时段：第二次被时段预检拦截返回 409")
    void sequentialDuplicateCreateSecondGets409() {
        assertEquals("success", tryCreate("13800000111"));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(request("13800000222")));

        assertEquals(409, ex.getStatus());
        assertEquals(SLOT_TAKEN_MESSAGE, ex.getMessage());
        assertEquals(1, countActiveRows());
    }

    @Test
    @DisplayName("booked -> cancelled 释放时段：取消后同桌位同时段可再次预约成功")
    void cancelReleasesSlotForRebooking() {
        Map<String, Object> first = service.create(request("13800000111"));
        long firstId = (Long) first.get("id");
        long ownerId = rows.get(firstId).getUserId();
        assertEquals(1, activeSlotIndex.size());

        // 时段被占用期间无法重复预约
        assertEquals(409, assertThrows(ApiException.class,
                () -> service.create(request("13800000222"))).getStatus());

        // 顾客取消本人预约：状态离开活跃集，时段释放
        Map<String, Object> cancelled = service.cancel(firstId, new AuthUser(ownerId, "测试顾客", "13800000111", "customer"));
        assertEquals("cancelled", cancelled.get("status"));
        assertEquals(0, activeSlotIndex.size(), "cancelled 不再占用时段");

        // 释放后同桌位同时段可再次预约
        Map<String, Object> second = service.create(request("13800000222"));
        assertNotNull(second.get("id"));
        assertEquals(1, countActiveRows(), "重新预约后有效预约仍只有一条");
        assertTrue(rows.size() >= 2, "取消的历史预约保留在库中");
    }
}
