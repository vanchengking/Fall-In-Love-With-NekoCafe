package com.nekocafe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nekocafe.common.Normalizer;
import com.nekocafe.entity.RecommendationLog;
import com.nekocafe.entity.User;
import com.nekocafe.mapper.RecommendationLogMapper;
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
 * 规则型推荐（D-01 选做1）：人-桌-菜-猫四维匹配评分。
 *
 * <p>偏好来源优先级：请求参数 {@code preferences} &gt; 用户档案 {@code users.preferences} &gt; 空数组。
 * 仅做本地规则评分，不引入任何外部 AI / 网络依赖。返回结构保持 {@code cat/tables/menuItems}
 * 三个字段以兼容前端，并为每个候选附加 {@code score} 与 {@code reasons}。</p>
 */
@Service
public class RecommendationService {

    private final CatalogService catalogService;
    private final UserMapper userMapper;
    private final RecommendationLogMapper recommendationLogMapper;
    private final ObjectMapper objectMapper;

    public RecommendationService(CatalogService catalogService,
                                 UserMapper userMapper,
                                 RecommendationLogMapper recommendationLogMapper,
                                 ObjectMapper objectMapper) {
        this.catalogService = catalogService;
        this.userMapper = userMapper;
        this.recommendationLogMapper = recommendationLogMapper;
        this.objectMapper = objectMapper;
    }

    /** 兼容既有 {@code /api/recommendations?userId=&storeId=&preferences=} 调用。 */
    public Map<String, Object> recommend(Long userId, Long storeId, List<String> preferences) {
        return recommend(userId, storeId, preferences, null, null, null);
    }

    /**
     * 四维推荐。当同时提供 {@code date/time/partySize} 时，桌位只推荐该时段仍可用的桌位。
     */
    public Map<String, Object> recommend(Long userId, Long storeId, List<String> preferences,
                                         String date, String time, Integer partySize) {
        final List<String> effective = resolvePreferences(userId, preferences);

        List<Map<String, Object>> cats = catalogService.listCats(storeId);
        List<Map<String, Object>> tables = catalogService.listTables(storeId,
                blankToNull(date), blankToNull(time), partySize);
        List<Map<String, Object>> menu = catalogService.listMenuItems(storeId);

        scoreCats(cats, effective);
        scoreTables(tables, effective, partySize, hasSlot(date, time));
        scoreMenu(menu, effective);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("preferences", effective);
        out.put("cat", cats.isEmpty() ? null : cats.get(0));
        out.put("tables", new ArrayList<>(tables.subList(0, Math.min(3, tables.size()))));
        out.put("menuItems", new ArrayList<>(menu.subList(0, Math.min(4, menu.size()))));

        writeLogQuietly(userId, storeId, effective, out);
        return out;
    }

    // ---------------------------------------------------------------------
    // 偏好来源优先级：请求参数 > 用户档案 > 空数组
    // ---------------------------------------------------------------------
    private List<String> resolvePreferences(Long userId, List<String> requested) {
        List<String> source = new ArrayList<>();
        if (requested != null && !requested.isEmpty()) {
            source = requested;
        } else if (userId != null) {
            User user = userMapper.selectById(userId);
            if (user != null && user.getPreferences() != null) {
                source = user.getPreferences();
            }
        }
        List<String> cleaned = new ArrayList<>();
        for (String p : source) {
            if (p != null && !p.isBlank() && !cleaned.contains(p.trim())) {
                cleaned.add(p.trim());
            }
        }
        return cleaned;
    }

    // ---------------------------------------------------------------------
    // 猫咪：性格标签命中 +10，healthy +2，interactive +2，非健康/休息降权
    // ---------------------------------------------------------------------
    private void scoreCats(List<Map<String, Object>> cats, List<String> prefs) {
        for (Map<String, Object> cat : cats) {
            int score = 0;
            List<String> reasons = new ArrayList<>();
            for (String hit : matched(prefs, asList(cat.get("personality_tags")))) {
                score += 10;
                reasons.add("匹配偏好：" + hit);
            }
            if ("healthy".equalsIgnoreCase(str(cat.get("health_status")))) {
                score += 2;
                reasons.add("健康状态正常");
            } else {
                score -= 3;
                reasons.add("健康状态需关注，已降权");
            }
            if ("interactive".equalsIgnoreCase(str(cat.get("interactive_status")))) {
                score += 2;
                reasons.add("可互动状态良好");
            } else if (cat.get("interactive_status") != null) {
                score -= 2;
                reasons.add("当前休息中，已降权");
            }
            cat.put("score", score);
            cat.put("reasons", reasons);
        }
        cats.sort(Comparator
                .comparingInt((Map<String, Object> c) -> intOf(c.get("score"))).reversed()
                .thenComparingLong(c -> Normalizer.toLong(c.get("id"))));
    }

    // ---------------------------------------------------------------------
    // 桌位：区域标签命中 +10，猫区 +3，容量刚好满足 +4；提供时段则仅推荐可用桌位
    // ---------------------------------------------------------------------
    private void scoreTables(List<Map<String, Object>> tables, List<String> prefs,
                             Integer partySize, boolean slotProvided) {
        if (slotProvided) {
            tables.removeIf(t -> Boolean.FALSE.equals(asBool(t.get("available_for_slot"))));
        }
        for (Map<String, Object> table : tables) {
            int score = 0;
            List<String> reasons = new ArrayList<>();
            boolean catZone = Normalizer.toBool(table.get("cat_zone"));
            List<String> tags = new ArrayList<>();
            tags.add(str(table.get("area")));
            tags.add(catZone ? "cat-zone" : "standard");
            for (String hit : matched(prefs, tags)) {
                score += 10;
                reasons.add("匹配偏好：" + hit);
            }
            if (catZone) {
                score += 3;
                reasons.add("猫区加分");
            }
            if (partySize != null) {
                long seats = Normalizer.toLong(table.get("seats"));
                if (seats == partySize) {
                    score += 4;
                    reasons.add("容量刚好满足");
                }
            }
            table.put("score", score);
            table.put("reasons", reasons);
        }
        tables.sort(Comparator
                .comparingInt((Map<String, Object> t) -> intOf(t.get("score"))).reversed()
                .thenComparingLong(t -> Normalizer.toLong(t.get("seats")))
                .thenComparingLong(t -> Normalizer.toLong(t.get("id"))));
    }

    // ---------------------------------------------------------------------
    // 菜品：标签命中 +10，仅推荐 available；同分按价格升序、id 升序稳定排序
    // ---------------------------------------------------------------------
    private void scoreMenu(List<Map<String, Object>> menu, List<String> prefs) {
        menu.removeIf(m -> !"available".equalsIgnoreCase(str(m.get("status"))));
        for (Map<String, Object> item : menu) {
            int score = 0;
            List<String> reasons = new ArrayList<>();
            for (String hit : matched(prefs, asList(item.get("tags")))) {
                score += 10;
                reasons.add("匹配偏好：" + hit);
            }
            item.put("score", score);
            item.put("reasons", reasons);
        }
        menu.sort(Comparator
                .comparingInt((Map<String, Object> m) -> intOf(m.get("score"))).reversed()
                .thenComparingLong(m -> Normalizer.toLong(m.get("price_cents")))
                .thenComparingLong(m -> Normalizer.toLong(m.get("id"))));
    }

    private void writeLogQuietly(Long userId, Long storeId, List<String> prefs, Map<String, Object> out) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            Object cat = out.get("cat");
            snapshot.put("cat", cat instanceof Map<?, ?> c ? c.get("id") : null);
            snapshot.put("tables", ids(out.get("tables")));
            snapshot.put("menuItems", ids(out.get("menuItems")));
            recommendationLogMapper.insert(new RecommendationLog(
                    userId, storeId,
                    objectMapper.writeValueAsString(prefs),
                    objectMapper.writeValueAsString(snapshot)));
        } catch (Exception ignored) {
            // 推荐日志失败不影响主流程
        }
    }

    private static List<Long> ids(Object value) {
        List<Long> out = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    out.add(Normalizer.toLong(m.get("id")));
                }
            }
        }
        return out;
    }

    /** 返回 preferences 中命中 candidateTags 的标签（小写、去重、保持偏好顺序）。 */
    private static List<String> matched(List<String> preferences, Collection<String> candidateTags) {
        Set<String> candidate = lower(candidateTags);
        List<String> result = new ArrayList<>();
        if (preferences != null) {
            for (String pref : preferences) {
                if (pref == null) {
                    continue;
                }
                String p = pref.trim().toLowerCase();
                if (!p.isEmpty() && candidate.contains(p) && !result.contains(p)) {
                    result.add(p);
                }
            }
        }
        return result;
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

    private static boolean hasSlot(String date, String time) {
        return date != null && !date.isBlank() && time != null && !time.isBlank();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int intOf(Object value) {
        return value instanceof Number n ? n.intValue() : (int) Normalizer.toLong(value);
    }

    private static Boolean asBool(Object value) {
        return value == null ? null : Normalizer.toBool(value);
    }
}
