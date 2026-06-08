package com.nekocafe.service;

import com.nekocafe.common.ApiException;
import com.nekocafe.mapper.StoreMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StoreAdminService {

    private final StoreMapper storeMapper;

    public StoreAdminService(StoreMapper storeMapper) {
        this.storeMapper = storeMapper;
    }

    public List<Map<String, Object>> listStores() {
        return storeMapper.listStores();
    }

    public Map<String, Object> getStoreDetail(Long id) {
        Map<String, Object> row = storeMapper.getStoreDetail(id);
        if (row == null || row.isEmpty()) {
            throw ApiException.notFound("store not found");
        }
        return row;
    }

    public Map<String, Object> createStore(Map<String, Object> body) {
        Map<String, Object> store = normalizeStore(body);
        storeMapper.insertStore(store);
        return getStoreDetail(toLong(store.get("id")));
    }

    public Map<String, Object> updateStore(Long id, Map<String, Object> body) {
        getStoreDetail(id);
        Map<String, Object> store = normalizeStore(body);
        store.put("id", id);
        if (storeMapper.updateStore(store) == 0) {
            throw ApiException.notFound("store not found");
        }
        return getStoreDetail(id);
    }

    public Map<String, Object> deleteStore(Long id) {
        Map<String, Object> existing = getStoreDetail(id);
        if (storeMapper.deleteStore(id) == 0) {
            throw ApiException.notFound("store not found");
        }
        return Map.of("deleted", true, "store", existing);
    }

    private static Map<String, Object> normalizeStore(Map<String, Object> body) {
        Map<String, Object> store = new LinkedHashMap<>();
        store.put("name", requiredString(body, "name"));
        store.put("city", requiredString(body, "city"));
        store.put("address", requiredString(body, "address"));
        store.put("phone", requiredString(body, "phone"));
        store.put("open_time", requiredString(body, "openTime", "open_time"));
        store.put("close_time", requiredString(body, "closeTime", "close_time"));
        store.put("latitude", optionalDecimal(body, "latitude"));
        store.put("longitude", optionalDecimal(body, "longitude"));
        store.put("business_hours_text", optionalString(body, "businessHoursText", "business_hours_text"));
        store.put("photo_url", optionalString(body, "photoUrl", "photo_url"));
        store.put("equipment_desc", optionalString(body, "equipmentDesc", "equipment_desc"));
        store.put("area_detail", optionalString(body, "areaDetail", "area_detail"));
        return store;
    }

    private static String requiredString(Map<String, Object> body, String... keys) {
        String value = optionalString(body, keys);
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(keys[0] + " is required");
        }
        return value;
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

    private static BigDecimal optionalDecimal(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw ApiException.badRequest(key + " must be a number");
        }
    }

    private static Long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
