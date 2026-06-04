package com.nekocafe.service;

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
 * 规则型推荐排序单元测试：偏好标签命中的猫咪/桌位应排在前。
 */
class RecommendationServiceTest {

    private Map<String, Object> cat(long id, List<String> tags, String health) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("personality_tags", new ArrayList<>(tags));
        m.put("health_status", health);
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
        return m;
    }

    @Test
    @DisplayName("偏好命中的安静猫咪被推荐为首选")
    void recommendsMatchingCat() {
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

        RecommendationService service = new RecommendationService(catalogService, userMapper);
        Map<String, Object> result = service.recommend(null, 1L, List.of("quiet"));

        @SuppressWarnings("unchecked")
        Map<String, Object> topCat = (Map<String, Object>) result.get("cat");
        assertNotNull(topCat);
        assertEquals(2L, ((Number) topCat.get("id")).longValue());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) result.get("tables");
        assertTrue(tables.size() <= 3);
        // cat-zone 桌位（+3）应排在最前
        assertEquals(11L, ((Number) tables.get(0).get("id")).longValue());
    }
}
