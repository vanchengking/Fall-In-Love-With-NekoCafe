package com.nekocafe.service;

import com.nekocafe.mapper.TableMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * FR-TABLE-003 桌位实时状态派生字段单元测试（验收标准：空闲 / 已预约 / 使用中 / 停用）。
 *
 * <p>口径：基础状态非 available 一律 disabled（停用优先于任何预约）；
 * 当前时段活跃预约 seated/dining 为 in_use，created/booked 为 reserved；
 * 无活跃预约且基础状态 available 为 free。中文标签随状态返回，预约状态附
 * current_reservation_status_label 中文。</p>
 */
@ExtendWith(MockitoExtension.class)
class TableAdminServiceRuntimeStatusTest {

    @Mock
    private TableMapper tableMapper;
    @Mock
    private CatalogService catalogService;

    private TableAdminService service;

    @BeforeEach
    void setUp() {
        service = new TableAdminService(tableMapper, catalogService);
    }

    private static Map<String, Object> tableRow(String baseStatus, String currentReservationStatus) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1L);
        row.put("store_id", 1L);
        row.put("code", "A01");
        row.put("seats", 2);
        row.put("area", "window");
        row.put("cat_zone", 1);
        row.put("status", baseStatus);
        row.put("available_for_slot", currentReservationStatus == null && "available".equals(baseStatus) ? 1 : 0);
        if (currentReservationStatus != null) {
            row.put("current_reservation_id", 66L);
            row.put("current_reservation_status", currentReservationStatus);
            row.put("reservation_time", "18:30");
            row.put("party_size", 2);
            row.put("customer_name", "林小喵");
            row.put("mobile_number", "13800000001");
        }
        return row;
    }

    private Map<String, Object> listSingle(String baseStatus, String currentReservationStatus) {
        when(tableMapper.listTables(any(), any(), any(), any()))
                .thenReturn(List.of(tableRow(baseStatus, currentReservationStatus)));
        return service.listTables(1L, "2026-06-12", "18:30", null, null, null, null).get(0);
    }

    @Test
    @DisplayName("无活跃预约且基础状态 available：free/空闲")
    void freeWhenNoActiveReservation() {
        Map<String, Object> row = listSingle("available", null);

        assertEquals("free", row.get("runtime_status"));
        assertEquals("空闲", row.get("runtime_status_label"));
        assertNull(row.get("current_reservation_status_label"));
        assertEquals(Boolean.TRUE, row.get("available_for_slot"));
    }

    @Test
    @DisplayName("booked/created 活跃预约：reserved/已预约")
    void reservedWhenBookedOrCreated() {
        Map<String, Object> booked = listSingle("available", "booked");
        assertEquals("reserved", booked.get("runtime_status"));
        assertEquals("已预约", booked.get("runtime_status_label"));
        assertEquals("已预约", booked.get("current_reservation_status_label"));

        Map<String, Object> created = listSingle("available", "created");
        assertEquals("reserved", created.get("runtime_status"));
        assertEquals("待确认", created.get("current_reservation_status_label"));
    }

    @Test
    @DisplayName("seated/dining 活跃预约：in_use/使用中")
    void inUseWhenSeatedOrDining() {
        Map<String, Object> seated = listSingle("available", "seated");
        assertEquals("in_use", seated.get("runtime_status"));
        assertEquals("使用中", seated.get("runtime_status_label"));
        assertEquals("已入座", seated.get("current_reservation_status_label"));

        Map<String, Object> dining = listSingle("available", "dining");
        assertEquals("in_use", dining.get("runtime_status"));
        assertEquals("用餐中", dining.get("current_reservation_status_label"));
    }

    @Test
    @DisplayName("基础状态非 available：disabled/停用，优先于任何预约")
    void disabledBeatsActiveReservation() {
        Map<String, Object> row = listSingle("maintenance", "booked");

        assertEquals("disabled", row.get("runtime_status"));
        assertEquals("停用", row.get("runtime_status_label"));
        assertEquals(Boolean.FALSE, row.get("available_for_slot"));
    }

    @Test
    @DisplayName("getTableDetail 同样派生实时状态字段")
    void tableDetailDerivesRuntimeStatusToo() {
        when(tableMapper.getTableDetail(anyLong(), any(), any()))
                .thenReturn(tableRow("available", "dining"));

        Map<String, Object> row = service.getTableDetail(1L, "2026-06-12", "18:30");

        assertEquals("in_use", row.get("runtime_status"));
        assertEquals("使用中", row.get("runtime_status_label"));
        assertEquals("用餐中", row.get("current_reservation_status_label"));
    }
}
