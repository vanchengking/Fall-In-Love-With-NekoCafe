package com.nekocafe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nekocafe.entity.User;
import com.nekocafe.mapper.RecommendationLogMapper;
import com.nekocafe.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 规则型推荐排序单元测试：偏好命中、猫区/容量加分、菜品标签、无偏好稳定排序、偏好优先级。
 */
class RecommendationServiceTest {

    private Map<String, Object> cat(long id, List<String> tags, String health) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("personality_tags", new ArrayList<>(tags));
        m.put("health_status", health);
        m.put("interactive_status", "interactive");
        return m;
    }

    private Map<String, Object> table(long id, String area, boolean catZone, int seats) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("area", area);
        m.put("cat_zone", catZone);
        m.put("seats", seats);
        return m;
    }

    private Map<String, Object> menu(long id, List<String> tags, int price) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("tags", new ArrayList<>(tags));
        m.put("price_cents", price);
        m.put("status", "available");
        return m;
    }

    private RecommendationService newService(CatalogService catalogService, UserMapper userMapper) {
        RecommendationLogMapper logMapper = mock(RecommendationLogMapper.class);
        return new RecommendationService(catalogService, userMapper, logMapper, new ObjectMapper());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> tablesOf(Map<String, Object> result) {
        return (List<Map<String, Object>>) result.get("tables");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> menuOf(Map<String, Object> result) {
        return (List<Map<String, Object>>) result.get("menuItems");
    }

    @Test
    @DisplayName("偏好命中的安静猫咪被推荐为首选，猫区桌位与标签菜品排在最前")
    void recommendsMatchingCatTableMenu() {
        CatalogService catalogService = mock(CatalogService.class);
        UserMapper userMapper = mock(UserMapper.class);

        when(catalogService.listCats(eq(1L))).thenReturn(new ArrayList<>(List.of(
                cat(1L, List.of("active"), "healthy"),
                cat(2L, List.of("quiet", "gentle"), "healthy"))));
        when(catalogService.listTables(eq(1L), any(), any(), any())).thenReturn(new ArrayList<>(List.of(
                table(10L, "main", false, 4),
                table(11L, "window", true, 2))));
        when(catalogService.listMenuItems(eq(1L))).thenReturn(new ArrayList<>(List.of(
                menu(100L, List.of("coffee"), 3200),
                menu(101L, List.of("quiet"), 2800))));

        RecommendationService service = newService(catalogService, userMapper);
        Map<String, Object> result = service.recommend(null, 1L, List.of("quiet"));

        @SuppressWarnings("unchecked")
        Map<String, Object> topCat = (Map<String, Object>) result.get("cat");
        assertNotNull(topCat);
        assertEquals(2L, ((Number) topCat.get("id")).longValue());

        List<Map<String, Object>> tables = tablesOf(result);
        assertTrue(tables.size() <= 3);
        // cat-zone 桌位（+3）应排在最前
        assertEquals(11L, ((Number) tables.get(0).get("id")).longValue());

        // 命中 quiet 标签的菜品应排在最前
        assertEquals(101L, ((Number) menuOf(result).get(0).get("id")).longValue());
    }

    @Test
    @DisplayName("无偏好时按稳定规则排序：猫按 id、桌按容量、菜按价格")
    void stableOrderWithoutPreferences() {
        CatalogService catalogService = mock(CatalogService.class);
        UserMapper userMapper = mock(UserMapper.class);

        when(catalogService.listCats(eq(1L))).thenReturn(new ArrayList<>(List.of(
                cat(2L, List.of(), "healthy"),
                cat(1L, List.of(), "healthy"))));
        when(catalogService.listTables(eq(1L), any(), any(), any())).thenReturn(new ArrayList<>(List.of(
                table(10L, "main", false, 4),
                table(11L, "main", false, 2))));
        when(catalogService.listMenuItems(eq(1L))).thenReturn(new ArrayList<>(List.of(
                menu(100L, List.of(), 3200),
                menu(101L, List.of(), 2800))));

        RecommendationService service = newService(catalogService, userMapper);
        Map<String, Object> result = service.recommend(null, 1L, new ArrayList<>());

        @SuppressWarnings("unchecked")
        Map<String, Object> topCat = (Map<String, Object>) result.get("cat");
        assertEquals(1L, ((Number) topCat.get("id")).longValue());            // 同分按 id 升序
        assertEquals(11L, ((Number) tablesOf(result).get(0).get("id")).longValue()); // 同分按容量升序
        assertEquals(101L, ((Number) menuOf(result).get(0).get("id")).longValue());  // 同分按价格升序
    }

    @Test
    @DisplayName("偏好优先级：请求参数覆盖用户档案")
    void requestPreferencesOverrideProfile() {
        CatalogService catalogService = mock(CatalogService.class);
        UserMapper userMapper = mock(UserMapper.class);

        User user = new User();
        user.setPreferences(new ArrayList<>(List.of("coffee")));
        when(userMapper.selectById(eq(1L))).thenReturn(user);
        when(catalogService.listCats(any())).thenReturn(new ArrayList<>());
        when(catalogService.listTables(any(), any(), any(), any())).thenReturn(new ArrayList<>());
        when(catalogService.listMenuItems(any())).thenReturn(new ArrayList<>());

        RecommendationService service = newService(catalogService, userMapper);

        // 提供请求偏好时，应使用请求偏好
        Map<String, Object> withRequest = service.recommend(1L, 1L, List.of("quiet"));
        assertEquals(List.of("quiet"), withRequest.get("preferences"));

        // 请求偏好为空时，回退用户档案偏好
        Map<String, Object> withProfile = service.recommend(1L, 1L, new ArrayList<>());
        assertEquals(List.of("coffee"), withProfile.get("preferences"));
    }

    @Test
    @DisplayName("提供时段时只推荐当前可用的桌位")
    void filtersUnavailableTablesWhenSlotProvided() {
        CatalogService catalogService = mock(CatalogService.class);
        UserMapper userMapper = mock(UserMapper.class);

        Map<String, Object> free = table(10L, "main", false, 2);
        free.put("available_for_slot", true);
        Map<String, Object> busy = table(11L, "window", true, 2);
        busy.put("available_for_slot", false);

        when(catalogService.listCats(any())).thenReturn(new ArrayList<>());
        when(catalogService.listTables(eq(1L), eq("2026-06-10"), eq("18:30"), eq(2)))
                .thenReturn(new ArrayList<>(List.of(free, busy)));
        when(catalogService.listMenuItems(any())).thenReturn(new ArrayList<>());

        RecommendationService service = newService(catalogService, userMapper);
        Map<String, Object> result = service.recommend(null, 1L, new ArrayList<>(), "2026-06-10", "18:30", 2);

        List<Map<String, Object>> tables = tablesOf(result);
        assertEquals(1, tables.size());
        assertEquals(10L, ((Number) tables.get(0).get("id")).longValue());
    }
}
