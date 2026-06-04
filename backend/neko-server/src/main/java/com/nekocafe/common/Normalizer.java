package com.nekocafe.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 行数据规范化：把 JSON 字符串列解析为数组、把 0/1 列转为布尔，
 * 使返回结构与前端期望一致（数组可 .join、布尔可 === false 判断）。
 */
public final class Normalizer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Normalizer() {
    }

    public static List<String> parseJsonArray(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                out.add(String.valueOf(o));
            }
            return out;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(text, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public static boolean toBool(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        String text = String.valueOf(value).trim();
        return text.equals("1") || text.equalsIgnoreCase("true");
    }

    public static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    /** 对每一行的指定列做 JSON 数组解析（就地替换值）。 */
    public static List<Map<String, Object>> arrayField(List<Map<String, Object>> rows, String key) {
        for (Map<String, Object> row : rows) {
            row.put(key, parseJsonArray(row.get(key)));
        }
        return rows;
    }

    /** 对每一行的指定列做布尔转换。 */
    public static List<Map<String, Object>> boolField(List<Map<String, Object>> rows, String key) {
        for (Map<String, Object> row : rows) {
            if (row.containsKey(key)) {
                row.put(key, toBool(row.get(key)));
            }
        }
        return rows;
    }
}
