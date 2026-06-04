package com.nekocafe.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nekocafe.common.Normalizer;
import com.nekocafe.mapper.CatalogMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 只读目录服务：门店（Redis 热点缓存）、桌位、菜品、猫咪及其健康/疫苗记录。
 */
@Service
public class CatalogService {

    private static final String STORES_CACHE_KEY = "neko:cache:stores";

    private final CatalogMapper catalogMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final long storesTtlSeconds;

    public CatalogService(CatalogMapper catalogMapper,
                          StringRedisTemplate redis,
                          ObjectMapper objectMapper,
                          @Value("${neko.cache.stores-ttl-seconds:60}") long storesTtlSeconds) {
        this.catalogMapper = catalogMapper;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.storesTtlSeconds = storesTtlSeconds;
    }

    /** 门店列表，使用 Redis 缓存热点查询，缓存失效或异常时回源数据库。 */
    public List<Map<String, Object>> listStores() {
        try {
            String cached = redis.opsForValue().get(STORES_CACHE_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception ignored) {
            // 缓存读取失败时回源
        }
        List<Map<String, Object>> stores = catalogMapper.listStores();
        try {
            redis.opsForValue().set(STORES_CACHE_KEY,
                    objectMapper.writeValueAsString(stores), storesTtlSeconds, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // 缓存写入失败不影响返回
        }
        return stores;
    }

    public List<Map<String, Object>> listTables(Long storeId, String date, String time, Integer partySize) {
        List<Map<String, Object>> rows = catalogMapper.listTables(storeId,
                emptyToNull(date), emptyToNull(time), partySize);
        Normalizer.boolField(rows, "cat_zone");
        Normalizer.boolField(rows, "available_for_slot");
        return rows;
    }

    public List<Map<String, Object>> listMenuItems(Long storeId) {
        return Normalizer.arrayField(catalogMapper.listMenuItems(storeId), "tags");
    }

    public List<Map<String, Object>> listCats(Long storeId) {
        return Normalizer.arrayField(catalogMapper.listCats(storeId), "personality_tags");
    }

    public List<Map<String, Object>> listCatHealthRecords(Long catId) {
        return catalogMapper.listCatHealthRecords(catId);
    }

    public List<Map<String, Object>> listVaccineRecords(Long catId) {
        return catalogMapper.listVaccineRecords(catId);
    }

    private static String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
