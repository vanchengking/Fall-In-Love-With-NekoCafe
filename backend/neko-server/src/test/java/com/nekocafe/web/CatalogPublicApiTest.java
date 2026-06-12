package com.nekocafe.web;

import com.nekocafe.config.SecurityConfig;
import com.nekocafe.mapper.CatalogMapper;
import com.nekocafe.security.JwtAuthFilter;
import com.nekocafe.security.JwtService;
import com.nekocafe.service.CatService;
import com.nekocafe.service.CatalogService;
import com.nekocafe.service.DashboardService;
import com.nekocafe.service.RecommendationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FR-STORE-001 / FR-STORE-002 公开接口契约测试：
 * GET /api/stores、GET /api/tables 匿名可访问，响应保持 {data:[...]} 结构，
 * 门店行透传 城市/地址/电话/营业时间/桌位统计，桌位行透传 seats/area/cat_zone/可用性，
 * 且 partySize 等筛选参数原样传递给服务层（容量过滤在 SQL 中执行）。
 */
@WebMvcTest(controllers = CatalogController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "neko.jwt.secret=test-secret-0123456789abcdef-0123456789abcdef",
        "neko.jwt.ttl-seconds=3600"
})
class CatalogPublicApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogService catalogService;
    @MockBean
    private CatService catService;
    @MockBean
    private RecommendationService recommendationService;
    @MockBean
    private DashboardService dashboardService;
    @MockBean
    private CatalogMapper catalogMapper;

    @Test
    @DisplayName("匿名 GET /api/stores：200，含城市/地址/电话/营业时间/桌位统计")
    void anonymousStoresListExposesFullContract() throws Exception {
        Map<String, Object> store = new LinkedHashMap<>();
        store.put("id", 3);
        store.put("name", "NekoCafe 朝阳大悦城店");
        store.put("city", "北京");
        store.put("address", "朝阳区朝阳北路 101 号");
        store.put("phone", "010-88880003");
        store.put("open_time", "10:00");
        store.put("close_time", "22:00");
        store.put("business_hours_text", "周一至周日 10:00-22:00");
        store.put("table_count", 4);
        store.put("total_seats", 16);
        when(catalogService.listStores()).thenReturn(List.of(store));

        mockMvc.perform(get("/api/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].city").value("北京"))
                .andExpect(jsonPath("$.data[0].address").value("朝阳区朝阳北路 101 号"))
                .andExpect(jsonPath("$.data[0].phone").value("010-88880003"))
                .andExpect(jsonPath("$.data[0].open_time").value("10:00"))
                .andExpect(jsonPath("$.data[0].close_time").value("22:00"))
                .andExpect(jsonPath("$.data[0].business_hours_text").value("周一至周日 10:00-22:00"))
                .andExpect(jsonPath("$.data[0].table_count").value(4))
                .andExpect(jsonPath("$.data[0].total_seats").value(16));
    }

    @Test
    @DisplayName("匿名 GET /api/tables：200，含容量/区域/猫区/可用性，筛选参数透传服务层")
    void anonymousTablesListExposesSeatsAreaCatZone() throws Exception {
        when(catalogService.listTables(3L, "2026-09-01", "19:00", 4, null))
                .thenReturn(List.of(tableRow(9, "W01", true)));

        mockMvc.perform(get("/api/tables")
                        .param("storeId", "3")
                        .param("date", "2026-09-01")
                        .param("time", "19:00")
                        .param("partySize", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(9))
                .andExpect(jsonPath("$.data[0].store_id").value(3))
                .andExpect(jsonPath("$.data[0].code").value("W01"))
                .andExpect(jsonPath("$.data[0].seats").value(4))
                .andExpect(jsonPath("$.data[0].area").value("window"))
                .andExpect(jsonPath("$.data[0].cat_zone").value(true))
                .andExpect(jsonPath("$.data[0].status").value("available"))
                .andExpect(jsonPath("$.data[0].available_for_slot").value(true));

        verify(catalogService).listTables(3L, "2026-09-01", "19:00", 4, null);
    }

    @Test
    @DisplayName("GET /api/tables?availableOnly=true：availableOnly 透传服务层")
    void tablesListPassesAvailableOnlyThrough() throws Exception {
        when(catalogService.listTables(3L, "2026-09-01", "19:00", 4, true))
                .thenReturn(List.of(tableRow(9, "W01", true)));

        mockMvc.perform(get("/api/tables")
                        .param("storeId", "3")
                        .param("date", "2026-09-01")
                        .param("time", "19:00")
                        .param("partySize", "4")
                        .param("availableOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].available_for_slot").value(true));

        verify(catalogService).listTables(3L, "2026-09-01", "19:00", 4, true);
    }

    @Test
    @DisplayName("不带 availableOnly 时：不可用桌位原样返回且 available_for_slot=false")
    void tablesListKeepsUnavailableRowsWithFlagFalse() throws Exception {
        when(catalogService.listTables(3L, "2026-09-01", "19:00", 4, null))
                .thenReturn(List.of(tableRow(10, "W02", false)));

        mockMvc.perform(get("/api/tables")
                        .param("storeId", "3")
                        .param("date", "2026-09-01")
                        .param("time", "19:00")
                        .param("partySize", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("W02"))
                .andExpect(jsonPath("$.data[0].available_for_slot").value(false));
    }

    private static Map<String, Object> tableRow(int id, String code, boolean availableForSlot) {
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("id", id);
        table.put("store_id", 3);
        table.put("code", code);
        table.put("seats", 4);
        table.put("area", "window");
        table.put("cat_zone", true);
        table.put("status", "available");
        table.put("available_for_slot", availableForSlot);
        return table;
    }
}
