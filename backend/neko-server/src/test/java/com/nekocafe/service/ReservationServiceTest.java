package com.nekocafe.service;

import com.nekocafe.common.ApiException;
import com.nekocafe.dto.ReservationRequest;
import com.nekocafe.dto.ReservationRescheduleRequest;
import com.nekocafe.entity.Reservation;
import com.nekocafe.entity.ReservationEvent;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ReservationService 状态流转单元测试：合法跳转写库+审计，非法跳转不写库不插事件，取消校验本人权限，
 * 完成预约发放积分（FR-MEMBER-002）。
 * 创建路径（FR-TABLE-002）：手动选桌逐项校验（404/400/409），时段占用与唯一约束兜底统一 409。
 */
class ReservationServiceTest {

    private ReservationMapper reservationMapper;
    private ReservationEventMapper reservationEventMapper;
    private UserMapper userMapper;
    private UserService userService;
    private ReservationService service;

    @BeforeEach
    void setup() {
        reservationMapper = mock(ReservationMapper.class);
        reservationEventMapper = mock(ReservationEventMapper.class);
        userMapper = mock(UserMapper.class);
        userService = mock(UserService.class);
        service = new ReservationService(reservationMapper, reservationEventMapper, userMapper, userService);
    }

    private Reservation reservation(long id, long userId, String status) {
        Reservation r = new Reservation();
        r.setId(id);
        r.setUserId(userId);
        r.setStoreId(1L);
        r.setStatus(status);
        return r;
    }

    @Test
    @DisplayName("合法跳转 booked->seated：写库、插事件并生成入座提醒，不发积分")
    void legalTransitionPersistsAndAudits() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "booked"));
        when(reservationMapper.getReservationDetail(1L)).thenReturn(new HashMap<>());

        service.updateStatus(1L, "seated", new AuthUser(2L, "店员", "138", "staff"));

        verify(reservationMapper, times(1)).update(isNull(), any());
        verify(reservationEventMapper, times(1)).insert(any(ReservationEvent.class));
        verify(reservationMapper, times(1)).insertSeatedAlert(eq(1L), eq(1L));
        verify(userService, never()).changePoints(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("dining->finished：状态写库 + 为预约用户发放完成积分并落流水")
    void finishAwardsPoints() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "dining"));
        when(reservationMapper.getReservationDetail(1L)).thenReturn(new HashMap<>());

        service.updateStatus(1L, "finished", new AuthUser(2L, "店员", "138", "staff"));

        verify(reservationMapper, times(1)).update(isNull(), any());
        verify(userService, times(1)).changePoints(
                eq(9L), eq(ReservationService.POINTS_PER_FINISHED_RESERVATION),
                eq("reservation_finished"), eq(1L), any());
    }

    @Test
    @DisplayName("finished 为终态：重复流转 finished 抛 400，不再重复发积分")
    void finishedIsTerminalNoDoubleAward() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "finished"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateStatus(1L, "finished", new AuthUser(2L, "店员", "138", "staff")));
        assertEquals(400, ex.getStatus());

        verify(reservationMapper, never()).update(any(), any());
        verify(userService, never()).changePoints(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("积分发放失败：异常向上传播（事务回滚，状态不停留在 finished 而积分缺失）")
    void awardFailurePropagatesForRollback() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "dining"));
        when(userService.changePoints(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any()))
                .thenThrow(ApiException.conflict("该来源的积分已发放，不可重复入账"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateStatus(1L, "finished", new AuthUser(2L, "店员", "138", "staff")));
        assertEquals(409, ex.getStatus());
    }

    @Test
    @DisplayName("非法跳转 booked->finished：抛 400，不写库、不插事件")
    void illegalTransitionRejected() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "booked"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateStatus(1L, "finished", new AuthUser(2L, "店员", "138", "staff")));
        assertEquals(400, ex.getStatus());

        verify(reservationMapper, never()).update(any(), any());
        verify(reservationEventMapper, never()).insert(any(ReservationEvent.class));
    }

    @Test
    @DisplayName("预约不存在：抛 404")
    void notFound() {
        when(reservationMapper.selectForUpdate(99L)).thenReturn(null);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateStatus(99L, "seated", new AuthUser(2L, "店员", "138", "staff")));
        assertEquals(404, ex.getStatus());
    }

    @Test
    @DisplayName("顾客取消本人 booked 预约：成功写库并插事件")
    void ownerCancelsOwnReservation() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "booked"));
        when(reservationMapper.getReservationDetail(1L)).thenReturn(new HashMap<>());

        service.cancel(1L, new AuthUser(9L, "顾客", "138", "customer"));

        verify(reservationMapper, times(1)).update(isNull(), any());
        verify(reservationEventMapper, times(1)).insert(any(ReservationEvent.class));
    }

    @Test
    @DisplayName("顾客取消他人预约：抛 403，不写库")
    void nonOwnerCannotCancel() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "booked"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.cancel(1L, new AuthUser(7L, "别人", "139", "customer")));
        assertEquals(403, ex.getStatus());

        verify(reservationMapper, never()).update(any(), any());
        verify(reservationEventMapper, never()).insert(any(ReservationEvent.class));
    }

    @Test
    @DisplayName("后台角色可代客取消他人预约")
    void staffCanCancelOnBehalf() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "booked"));
        when(reservationMapper.getReservationDetail(1L)).thenReturn(new HashMap<>());

        service.cancel(1L, new AuthUser(2L, "店员", "138", "staff"));

        verify(reservationMapper, times(1)).update(isNull(), any());
        verify(reservationEventMapper, times(1)).insert(any(ReservationEvent.class));
    }

    // ------------------------------------------------------------------
    // FR-TABLE-002 创建路径：手动选桌校验、时段占用预检、唯一约束兜底转 409
    // ------------------------------------------------------------------

    private static final String SLOT_TAKEN_MESSAGE = "该桌位在该时段已被预约，请重新选择桌位或时间";

    private static ReservationRequest createRequest(Long tableId) {
        String tomorrow = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1).toString();
        return new ReservationRequest("林小满", "13800000001", 1L, tableId, null,
                tomorrow, "18:30", 2, null, null);
    }

    private static Map<String, Object> tableRow(long id, long storeId, int seats, String status) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("store_id", storeId);
        row.put("code", "A01");
        row.put("seats", seats);
        row.put("status", status);
        return row;
    }

    private void givenExistingCustomer() {
        User user = new User();
        user.setId(9L);
        user.setRole("customer");
        when(userMapper.selectOne(any())).thenReturn(user);
    }

    @Test
    @DisplayName("手动选桌创建成功：锁桌→校验→预检→插入，并写 created/booked 两条审计事件")
    void manualCreateHappyPath() {
        givenExistingCustomer();
        when(reservationMapper.lockTableById(5L)).thenReturn(tableRow(5L, 1L, 4, "available"));
        when(reservationMapper.countActiveOnSlot(eq(5L), anyString(), anyString())).thenReturn(0);
        when(reservationMapper.insert(any(Reservation.class))).thenAnswer(inv -> {
            inv.getArgument(0, Reservation.class).setId(100L);
            return 1;
        });
        when(reservationMapper.getReservationDetail(100L))
                .thenReturn(new HashMap<>(Map.of("id", 100L, "status", "booked")));

        Map<String, Object> detail = service.create(createRequest(5L));

        assertEquals("booked", detail.get("status"));
        verify(reservationMapper, times(1)).insert(any(Reservation.class));
        verify(reservationEventMapper, times(2)).insert(any(ReservationEvent.class));
    }

    @Test
    @DisplayName("并发兜底：插入命中 uq_reservations_active_slot 唯一约束转 409 且文案符合验收口径")
    void duplicateKeyOnActiveSlotBecomes409() {
        givenExistingCustomer();
        when(reservationMapper.lockTableById(5L)).thenReturn(tableRow(5L, 1L, 4, "available"));
        when(reservationMapper.countActiveOnSlot(eq(5L), anyString(), anyString())).thenReturn(0);
        when(reservationMapper.insert(any(Reservation.class))).thenThrow(new DuplicateKeyException(
                "Duplicate entry '5#2026-06-13#18:30:00' for key 'reservations.uq_reservations_active_slot'"));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(createRequest(5L)));

        assertEquals(409, ex.getStatus());
        assertEquals(SLOT_TAKEN_MESSAGE, ex.getMessage());
        verify(reservationEventMapper, never()).insert(any(ReservationEvent.class));
    }

    @Test
    @DisplayName("并发兜底：命中 uq_reservations_user_slot（同人同店同时段）也转 409，文案提示重复预约")
    void duplicateKeyOnUserSlotBecomes409() {
        givenExistingCustomer();
        when(reservationMapper.lockTableById(5L)).thenReturn(tableRow(5L, 1L, 4, "available"));
        when(reservationMapper.countActiveOnSlot(eq(5L), anyString(), anyString())).thenReturn(0);
        when(reservationMapper.insert(any(Reservation.class))).thenThrow(new DuplicateKeyException(
                "Duplicate entry '9#1#2026-06-13#18:30:00' for key 'reservations.uq_reservations_user_slot'"));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(createRequest(5L)));

        assertEquals(409, ex.getStatus());
        assertEquals("您在该门店该时段已有有效预约，请勿重复预约", ex.getMessage());
    }

    @Test
    @DisplayName("时段已被活跃预约占用：预检直接 409，不执行插入")
    void occupiedSlotRejectedBeforeInsert() {
        givenExistingCustomer();
        when(reservationMapper.lockTableById(5L)).thenReturn(tableRow(5L, 1L, 4, "available"));
        when(reservationMapper.countActiveOnSlot(eq(5L), anyString(), anyString())).thenReturn(1);

        ApiException ex = assertThrows(ApiException.class, () -> service.create(createRequest(5L)));

        assertEquals(409, ex.getStatus());
        assertEquals(SLOT_TAKEN_MESSAGE, ex.getMessage());
        verify(reservationMapper, never()).insert(any(Reservation.class));
    }

    @Test
    @DisplayName("手动选桌：桌位不存在返回 404 而非 500")
    void manualTableNotFoundIs404() {
        givenExistingCustomer();
        when(reservationMapper.lockTableById(404L)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class, () -> service.create(createRequest(404L)));

        assertEquals(404, ex.getStatus());
        verify(reservationMapper, never()).insert(any(Reservation.class));
    }

    @Test
    @DisplayName("手动选桌：桌位不属于请求门店返回 400")
    void manualTableStoreMismatchIs400() {
        givenExistingCustomer();
        when(reservationMapper.lockTableById(5L)).thenReturn(tableRow(5L, 2L, 4, "available"));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(createRequest(5L)));

        assertEquals(400, ex.getStatus());
        verify(reservationMapper, never()).insert(any(Reservation.class));
    }

    @Test
    @DisplayName("手动选桌：已停用桌位返回 409 而非 500")
    void manualTableDisabledIs409() {
        givenExistingCustomer();
        when(reservationMapper.lockTableById(5L)).thenReturn(tableRow(5L, 1L, 4, "disabled"));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(createRequest(5L)));

        assertEquals(409, ex.getStatus());
        verify(reservationMapper, never()).insert(any(Reservation.class));
    }

    @Test
    @DisplayName("手动选桌：容量不足返回 409")
    void manualTableTooSmallIs409() {
        givenExistingCustomer();
        when(reservationMapper.lockTableById(5L)).thenReturn(tableRow(5L, 1L, 1, "available"));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(createRequest(5L)));

        assertEquals(409, ex.getStatus());
        verify(reservationMapper, never()).insert(any(Reservation.class));
    }

    @Test
    @DisplayName("自动分配：无满足条件的空闲桌位返回 409")
    void autoAssignNoTableIs409() {
        givenExistingCustomer();
        when(reservationMapper.findAvailableTableForUpdate(eq(1L), eq(2), anyString(), anyString()))
                .thenReturn(null);

        ApiException ex = assertThrows(ApiException.class, () -> service.create(createRequest(null)));

        assertEquals(409, ex.getStatus());
        verify(reservationMapper, never()).insert(any(Reservation.class));
    }

    @Test
    @DisplayName("参数非法（人数超限）返回 400，不触达任何数据库写入")
    void invalidPartySizeIs400() {
        ReservationRequest bad = new ReservationRequest("林小满", "13800000001", 1L, 5L, null,
                LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1).toString(), "18:30", 99, null, null);

        ApiException ex = assertThrows(ApiException.class, () -> service.create(bad));

        assertEquals(400, ex.getStatus());
        verify(reservationMapper, never()).insert(any(Reservation.class));
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("并发建档兜底：users 唯一约束命中后重读用户继续创建，而非 500")
    void concurrentCustomerUpsertFallsBackToReselect() {
        User winner = new User();
        winner.setId(9L);
        winner.setRole("customer");
        // 第一次查询不存在 → 插入命中唯一约束 → 重读拿到对方已建档的用户
        when(userMapper.selectOne(any())).thenReturn(null, winner);
        when(userMapper.insert(any(User.class))).thenThrow(new DuplicateKeyException(
                "Duplicate entry '13800000001' for key 'users.mobile_number'"));
        when(reservationMapper.lockTableById(5L)).thenReturn(tableRow(5L, 1L, 4, "available"));
        when(reservationMapper.countActiveOnSlot(eq(5L), anyString(), anyString())).thenReturn(0);
        when(reservationMapper.insert(any(Reservation.class))).thenAnswer(inv -> {
            inv.getArgument(0, Reservation.class).setId(101L);
            return 1;
        });
        when(reservationMapper.getReservationDetail(101L))
                .thenReturn(new HashMap<>(Map.of("id", 101L, "status", "booked")));

        Map<String, Object> detail = service.create(createRequest(5L));

        assertEquals(101L, detail.get("id"));
        verify(reservationMapper, times(1)).insert(any(Reservation.class));
    }

    @Test
    @DisplayName("顾客可改约本人 booked 预约：成功更新时段并写入事件")
    void ownerCanRescheduleOwnReservation() {
        Reservation current = reservation(1L, 9L, "booked");
        current.setTableId(3L);
        when(reservationMapper.selectForUpdate(1L)).thenReturn(current);
        when(reservationMapper.findAvailableTableForUpdate(1L, 4, "2099-12-31", "19:00"))
                .thenReturn(new HashMap<>(java.util.Map.of("id", 8L)));
        when(reservationMapper.countActiveOnSlotExcludingReservation(1L, 8L, "2099-12-31", "19:00"))
                .thenReturn(0);
        when(reservationMapper.rescheduleReservation(any(Reservation.class))).thenReturn(1);
        when(reservationMapper.getReservationDetail(1L)).thenReturn(new HashMap<>());

        service.reschedule(1L, new ReservationRescheduleRequest(
                1L, null, null, "2099-12-31", "19:00", 4, "改到晚一点"), new AuthUser(9L, "顾客", "138", "customer"));

        verify(reservationMapper, times(1)).rescheduleReservation(any(Reservation.class));
        verify(reservationEventMapper, times(1)).insert(any(ReservationEvent.class));
    }

    @Test
    @DisplayName("顾客不能改约他人预约：抛 403")
    void nonOwnerCannotReschedule() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "booked"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.reschedule(1L, new ReservationRescheduleRequest(
                        1L, null, null, "2099-12-31", "19:00", 2, null),
                        new AuthUser(7L, "别人", "139", "customer")));
        assertEquals(403, ex.getStatus());

        verify(reservationMapper, never()).rescheduleReservation(any(Reservation.class));
    }
}
