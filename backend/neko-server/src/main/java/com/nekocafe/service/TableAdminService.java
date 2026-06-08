package com.nekocafe.service;

import com.nekocafe.common.ApiException;
import com.nekocafe.common.Normalizer;
import com.nekocafe.mapper.TableMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TableAdminService {

    private final TableMapper tableMapper;

    public TableAdminService(TableMapper tableMapper) {
        this.tableMapper = tableMapper;
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
        return row;
    }

    public Map<String, Object> createTable(Map<String, Object> body) {
        Map<String, Object> table = normalizeTable(body);
        tableMapper.insertTable(table);
        return getTableDetail(toLong(table.get("id")), null, null);
    }

    public Map<String, Object> updateTable(Long id, Map<String, Object> body) {
        getTableDetail(id, null, null);
        Map<String, Object> table = normalizeTable(body);
        table.put("id", id);
        if (tableMapper.updateTable(table) == 0) {
            throw ApiException.notFound("table not found");
        }
        return getTableDetail(id, null, null);
    }

    public Map<String, Object> deleteTable(Long id) {
        Map<String, Object> existing = getTableDetail(id, null, null);
        if (tableMapper.deleteTable(id) == 0) {
            throw ApiException.notFound("table not found");
        }
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
