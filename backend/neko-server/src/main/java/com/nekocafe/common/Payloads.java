package com.nekocafe.common;

import java.util.Map;

/**
 * 解开前端 {data: {...}} 包装；若无 data 字段则返回原始 body（兼容 req.body.data || req.body）。
 */
public final class Payloads {

    private Payloads() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> unwrap(Map<String, Object> body) {
        if (body == null) {
            return Map.of();
        }
        Object data = body.get("data");
        if (data instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return body;
    }
}
