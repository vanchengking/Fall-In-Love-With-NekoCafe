package com.nekocafe.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FR-TABLE-002 说明性测试：静态校验 ReservationMapper 注解 SQL 的并发保障口径，固化验收标准——
 * 自动分配必须 FOR UPDATE SKIP LOCKED 且只选 status='available'、容量足够、时段未被活跃预约占用的桌位，
 * 排序保持「优先猫区、容量最小满足、桌号稳定」；时段计数的活跃状态只含 created/booked/seated/dining；
 * 手动选桌按 id 行锁（门店/容量/状态由服务层区分 404/400/409）。
 */
class ReservationMapperSlotSqlTest {

    private static final String ACTIVE_STATUS_SET = "status IN ('created', 'booked', 'seated', 'dining')";

    @Test
    @DisplayName("自动分配：SKIP LOCKED 行锁 + available 桌位 + 活跃预约排除 + 猫区/容量/桌号稳定排序")
    void autoAssignSqlLocksAndOrdersDeterministically() throws Exception {
        String sql = selectSqlOf("findAvailableTableForUpdate",
                Long.class, Integer.class, String.class, String.class);

        assertTrue(sql.contains("FOR UPDATE SKIP LOCKED"),
                "自动分配必须用 SKIP LOCKED 锁定候选桌位，避免并发分配到同一桌");
        assertTrue(sql.contains("t.status = 'available'"), "只可分配基础状态 available 的桌位");
        assertTrue(sql.contains("t.seats >= #{partySize}"), "容量必须满足人数");
        assertTrue(sql.contains("r." + ACTIVE_STATUS_SET), "时段占用判断只认 created/booked/seated/dining");
        assertTrue(sql.contains("ORDER BY t.cat_zone DESC, t.seats ASC, t.code ASC"),
                "排序口径必须保持：优先猫区、容量最小满足、桌号稳定排序");
        assertTrue(sql.contains("LIMIT 1"), "一次只锁定并返回一张候选桌");
    }

    @Test
    @DisplayName("手动选桌：lockTableById 仅按 id 行锁，门店/容量/状态留给服务层逐项校验")
    void manualLockSqlLocksRowByIdOnly() throws Exception {
        String sql = selectSqlOf("lockTableById", Long.class);

        assertTrue(sql.contains("FOR UPDATE"), "手动选桌必须锁定目标桌位行");
        assertTrue(sql.contains("WHERE id = #{tableId}"), "按 id 定位桌位行");
        assertFalse(sql.contains("store_id = #{storeId}") || sql.contains("seats >=") || sql.contains("status ="),
                "不得在 SQL 中合并过滤门店/容量/状态，否则无法区分 404/400/409");
    }

    @Test
    @DisplayName("时段计数：countActiveOnSlot 仅统计活跃状态，终态不占用时段")
    void slotCountSqlUsesActiveStatusesOnly() throws Exception {
        String sql = selectSqlOf("countActiveOnSlot", Long.class, String.class, String.class);

        assertTrue(sql.contains(ACTIVE_STATUS_SET), "占用时段的状态必须且只能是 created/booked/seated/dining");
        assertFalse(sql.contains("cancelled") || sql.contains("finished") || sql.contains("no_show"),
                "cancelled/finished/no_show 已释放时段，不得出现在占用判断中");
    }

    @Test
    @DisplayName("改约时段计数：排除自身后口径与 countActiveOnSlot 一致")
    void rescheduleSlotCountSharesTheSameContract() throws Exception {
        String sql = selectSqlOf("countActiveOnSlotExcludingReservation",
                Long.class, Long.class, String.class, String.class);

        assertTrue(sql.contains(ACTIVE_STATUS_SET));
        assertTrue(sql.contains("id <> #{reservationId}"), "改约必须排除自身预约");
    }

    @Test
    @DisplayName("状态机流转：selectForUpdate 行锁防并发流转")
    void transitionReadLocksReservationRow() throws Exception {
        String sql = selectSqlOf("selectForUpdate", Long.class);
        assertTrue(sql.contains("FOR UPDATE"));
    }

    private static String selectSqlOf(String method, Class<?>... paramTypes) throws Exception {
        Select select = ReservationMapper.class.getMethod(method, paramTypes).getAnnotation(Select.class);
        return String.join("\n", select.value());
    }
}
