package com.nekocafe.service;

import com.nekocafe.common.Normalizer;
import com.nekocafe.entity.User;
import com.nekocafe.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 规则型推荐（D-01 选做1）：基于用户偏好标签，对猫咪性格、桌位区域、菜品标签做加权评分排序。
 */
@Service
public class RecommendationService {

    private final CatalogService catalogService;
    private final UserMapper userMapper;

    public RecommendationService(CatalogService catalogService, UserMapper userMapper) {
        this.catalogService = catalogService;
        this.userMapper = userMapper;
    }

    public Map<String, Object> recommend(Long userId, Long storeId, List<String> preferences) {
        List<String> prefs = (preferences == null) ? new ArrayList<>() : new ArrayList<>(preferences);
        if (userId != null) {
            User user = userMapper.selectById(userId);
            if (user != null && user.getPreferences() != null && !user.getPreferences().isEmpty()) {
                prefs = new ArrayList<>(user.getPreferences());
            }
        }
        final List<String> effective = prefs;

        List<Map<String, Object>> cats = catalogService.listCats(storeId);
        List<Map<String, Object>> tables = catalogService.listTables(storeId, null, null, null);
        List<Map<String, Object>> menu = catalogService.listMenuItems(storeId);

        for (Map<String, Object> cat : cats) {
            int score = scoreByTags(effective, asList(cat.get("personality_tags")));
            if ("healthy".equals(cat.get("health_status"))) {
                score += 2;
            }
            cat.put("score", score);
        }
        cats.sort(Comparator
                .comparingInt((Map<String, Object> c) -> ((Number) c.get("score")).intValue()).reversed()
                .thenComparingLong(c -> Normalizer.toLong(c.get("id"))));

        for (Map<String, Object> table : tables) {
            boolean catZone = Normalizer.toBool(table.get("cat_zone"));
            List<String> tags = new ArrayList<>();
            tags.add(String.valueOf(table.get("area")));
            tags.add(catZone ? "cat-zone" : "standard");
            int score = scoreByTags(effective, tags) + (catZone ? 3 : 0);
            table.put("score", score);
        }
        tables.sort(Comparator
                .comparingInt((Map<String, Object> t) -> ((Number) t.get("score")).intValue()).reversed()
                .thenComparingLong(t -> Normalizer.toLong(t.get("seats"))));

        for (Map<String, Object> item : menu) {
            item.put("score", scoreByTags(effective, asList(item.get("tags"))));
        }
        menu.sort(Comparator
                .comparingInt((Map<String, Object> m) -> ((Number) m.get("score")).intValue()).reversed()
                .thenComparingLong(m -> Normalizer.toLong(m.get("price_cents"))));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("preferences", effective);
        out.put("cat", cats.isEmpty() ? null : cats.get(0));
        out.put("tables", tables.subList(0, Math.min(3, tables.size())));
        out.put("menuItems", menu.subList(0, Math.min(4, menu.size())));
        return out;
    }

    private static int scoreByTags(Collection<String> preferences, Collection<String> candidateTags) {
        Set<String> preferred = lower(preferences);
        Set<String> candidate = lower(candidateTags);
        int score = 0;
        for (String tag : preferred) {
            if (candidate.contains(tag)) {
                score += 10;
            }
        }
        return score;
    }

    private static Set<String> lower(Collection<String> tags) {
        Set<String> out = new HashSet<>();
        if (tags != null) {
            for (String t : tags) {
                if (t != null) {
                    out.add(t.toLowerCase());
                }
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<String> asList(Object value) {
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                out.add(String.valueOf(o));
            }
            return out;
        }
        return Normalizer.parseJsonArray(value);
    }
}
