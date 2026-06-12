package com.nekocafe.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FR-TABLE-001 说明性测试：静态校验 TableMapper 注解 SQL 的时段可用性口径，
 * 固化验收标准——占用时段的活跃预约状态只包含 created/booked/seated/dining
 * （cancelled/finished/no_show 不占用），available_for_slot 必须同时要求桌位
 * status='available'，partySize 按 seats >= partySize 过滤，且 date/time 均为空时
 * 不做时段判断（避免无时段查询被任意历史预约误判为占用）。
 */
class TableMapperSlotSqlTest {

    private static final String ACTIVE_STATUS_SET = "r.status IN ('created', 'booked', 'seated', 'dining')";

    @Test
    @DisplayName("listTables：活跃状态集合、桌位状态与容量过滤、空时段守卫")
    void listTablesSqlEnforcesSlotAvailabilityContract() throws Exception {
        String sql = selectSqlOf("listTables", Long.class, String.class, String.class, Integer.class);

        assertTrue(sql.contains(ACTIVE_STATUS_SET), "占用时段的活跃状态必须且只能是 created/booked/seated/dining");
        assertFalse(sql.contains("cancelled") || sql.contains("finished") || sql.contains("no_show"),
                "cancelled/finished/no_show 不得出现在占用判断中");
        assertTrue(sql.contains("t.status = 'available'"), "available_for_slot 必须要求桌位自身 status=available");
        assertTrue(sql.contains("t.seats >= #{partySize}"), "partySize 必须按 seats >= partySize 过滤");
        assertTrue(sql.contains("#{date} IS NULL AND #{time} IS NULL"),
                "date/time 均为空时跳过时段判断，只传其一时按该字段保守判占用");
    }

    @Test
    @DisplayName("getTableDetail：available_for_slot 口径与 listTables 一致")
    void tableDetailSqlSharesTheSameAvailabilityContract() throws Exception {
        String sql = selectSqlOf("getTableDetail", Long.class, String.class, String.class);

        assertTrue(sql.contains(ACTIVE_STATUS_SET));
        assertTrue(sql.contains("t.status = 'available'"));
        assertTrue(sql.contains("#{date} IS NULL AND #{time} IS NULL"));
    }

    private static String selectSqlOf(String method, Class<?>... paramTypes) throws Exception {
        Select select = TableMapper.class.getMethod(method, paramTypes).getAnnotation(Select.class);
        return String.join("\n", select.value());
    }

    /** 编译期校验方法签名未被破坏（返回类型仍为行 Map）。 */
    @SuppressWarnings("unused")
    private static void signatureGuard(TableMapper mapper) {
        List<Map<String, Object>> rows = mapper.listTables(1L, "2026-09-01", "18:30", 2);
        Map<String, Object> row = mapper.getTableDetail(1L, "2026-09-01", "18:30");
    }
}
