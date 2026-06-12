package com.nekocafe.service;

import com.nekocafe.common.ApiException;
import com.nekocafe.common.Normalizer;
import com.nekocafe.domain.ReservationStatus;
import com.nekocafe.mapper.TableMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TableAdminService {

    /** FR-TABLE-003 实时状态中文标签（只读派生字段，不写库）。 */
    private static final Map<String, String> RUNTIME_STATUS_LABELS = Map.of(
            "free", "空闲",
            "reserved", "已预约",
            "in_use", "使用中",
            "disabled", "停用");

    private final TableMapper tableMapper;
    private final CatalogService catalogService;

    public TableAdminService(TableMapper tableMapper, CatalogService catalogService) {
        this.tableMapper = tableMapper;
        this.catalogService = catalogService;
    }

    public List<Map<String, Object>> listTables(Long storeId,
                                                String date,
                                                String time,
                                                Integer partySize,
                                                String area,
                                                String status,
                                                Boolean availableOnly) {
        List<Map<String, Object>> rows = tableMapper.listTables(storeId, emptyToNull(date), emptyToNull(time), partySize);
        Normalizer.boolField(rows, "cat_zone");
        Normalizer.boolField(rows, "available_for_slot");
        rows.forEach(TableAdminService::applyRuntimeStatus);
        return rows.stream()
                .filter(row -> matches(row.get("area"), area))
                .filter(row -> matches(row.get("status"), status))
                .filter(row -> availableOnly == null || !availableOnly || Boolean.TRUE.equals(row.get("available_for_slot")))
                .toList();
    }

    public Map<String, Object> getTableDetail(Long id, String date, String time) {
        Map<String, Object> row = tableMapper.getTableDetail(id, emptyToNull(date), emptyToNull(time));
        if (row == null || row.isEmpty()) {
            throw ApiException.notFound("table not found");
        }
        Normalizer.boolField(List.of(row), "cat_zone");
        Normalizer.boolField(List.of(row), "available_for_slot");
        applyRuntimeStatus(row);
        return row;
    }

    /**
     * FR-TABLE-003 桌位实时状态口径（以查询的 date/time/storeId 为准）：
     * 基础状态非 available 一律 disabled（停用优先于任何预约）；
     * 当前时段活跃预约 seated/dining 为 in_use，created/booked 为 reserved；
     * 无活跃预约且基础状态 available 为 free。
     */
    private static void applyRuntimeStatus(Map<String, Object> row) {
        String base = row.get("status") == null ? "available" : String.valueOf(row.get("status"));
        Object rawCurrent = row.get("current_reservation_status");
        String current = rawCurrent == null ? null : String.valueOf(rawCurrent);
        String runtime;
        if (!"available".equalsIgnoreCase(base)) {
            runtime = "disabled";
        } else if ("seated".equalsIgnoreCase(current) || "dining".equalsIgnoreCase(current)) {
            runtime = "in_use";
        } else if (current != null) {
            runtime = "reserved";
        } else {
            runtime = "free";
        }
        row.put("runtime_status", runtime);
        row.put("runtime_status_label", RUNTIME_STATUS_LABELS.get(runtime));
        row.put("current_reservation_status_label", current == null ? null : ReservationStatus.labelOf(current));
    }

    public Map<String, Object> createTable(Map<String, Object> body) {
        Map<String, Object> table = normalizeTable(body);
        tableMapper.insertTable(table);
        // 桌位增删改会改变门店列表的 table_count/total_seats 统计，需要同步失效缓存
        catalogService.evictStoresCache();
        return getTableDetail(toLong(table.get("id")), null, null);
    }

    public Map<String, Object> updateTable(Long id, Map<String, Object> body) {
        getTableDetail(id, null, null);
        Map<String, Object> table = normalizeTable(body);
        table.put("id", id);
        if (tableMapper.updateTable(table) == 0) {
            throw ApiException.notFound("table not found");
        }
        catalogService.evictStoresCache();
        return getTableDetail(id, null, null);
    }

    public Map<String, Object> deleteTable(Long id) {
        Map<String, Object> existing = getTableDetail(id, null, null);
        if (tableMapper.deleteTable(id) == 0) {
            throw ApiException.notFound("table not found");
        }
        catalogService.evictStoresCache();
        return Map.of("deleted", true, "table", existing);
    }

    private static boolean matches(Object actual, String expected) {
        return expected == null || expected.isBlank() || String.valueOf(actual).equalsIgnoreCase(expected.trim());
    }

    private static Map<String, Object> normalizeTable(Map<String, Object> body) {
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("store_id", requiredLong(body, "storeId", "store_id"));
        table.put("code", requiredString(body, "code"));
        table.put("seats", requiredInteger(body, "seats"));
        table.put("area", requiredString(body, "area"));
        table.put("cat_zone", optionalBoolean(body, "catZone", "cat_zone", false));
        table.put("status", defaultString(body, "available", "status"));
        table.put("photo_url", optionalString(body, "photoUrl", "photo_url"));
        table.put("area_detail", optionalString(body, "areaDetail", "area_detail"));
        table.put("device_note", optionalString(body, "deviceNote", "device_note"));
        return table;
    }

    private static String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private static String requiredString(Map<String, Object> body, String... keys) {
        String value = optionalString(body, keys);
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(keys[0] + " is required");
        }
        return value;
    }

    private static String defaultString(Map<String, Object> body, String defaultValue, String... keys) {
        String value = optionalString(body, keys);
        return value == null ? defaultValue : value;
    }

    private static String optionalString(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            Object value = body.get(key);
            if (value != null) {
                String str = String.valueOf(value).trim();
                return str.isEmpty() ? null : str;
            }
        }
        return null;
    }

    private static Long requiredLong(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            Object value = body.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                try {
                    return Long.valueOf(String.valueOf(value).trim());
                } catch (NumberFormatException ex) {
                    throw ApiException.badRequest(key + " must be an integer");
                }
            }
        }
        throw ApiException.badRequest(keys[0] + " is required");
    }

    private static Integer requiredInteger(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw ApiException.badRequest(key + " is required");
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw ApiException.badRequest(key + " must be an integer");
        }
    }

    private static Boolean optionalBoolean(Map<String, Object> body, String key1, String key2, boolean defaultValue) {
        Object value = body.containsKey(key1) ? body.get(key1) : body.get(key2);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String str = String.valueOf(value).trim();
        if ("true".equalsIgnoreCase(str) || "1".equals(str)) {
            return true;
        }
        if ("false".equalsIgnoreCase(str) || "0".equals(str)) {
            return false;
        }
        throw ApiException.badRequest(key1 + " must be a boolean");
    }

    private static Long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
