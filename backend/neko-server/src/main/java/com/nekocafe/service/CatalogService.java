package com.nekocafe.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nekocafe.common.Normalizer;
import com.nekocafe.mapper.CatalogMapper;
import com.nekocafe.mapper.StoreMapper;
import com.nekocafe.mapper.TableMapper;
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
    private final StoreMapper storeMapper;
    private final TableMapper tableMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final long storesTtlSeconds;

    public CatalogService(CatalogMapper catalogMapper,
                          StoreMapper storeMapper,
                          TableMapper tableMapper,
                          StringRedisTemplate redis,
                          ObjectMapper objectMapper,
                          @Value("${neko.cache.stores-ttl-seconds:60}") long storesTtlSeconds) {
        this.catalogMapper = catalogMapper;
        this.storeMapper = storeMapper;
        this.tableMapper = tableMapper;
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
        List<Map<String, Object>> stores = storeMapper.listStores();
        try {
            redis.opsForValue().set(STORES_CACHE_KEY,
                    objectMapper.writeValueAsString(stores), storesTtlSeconds, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // 缓存写入失败不影响返回
        }
        return stores;
    }

    /** 门店/桌位管理端变更后调用：清除门店列表缓存，避免 TTL 内读到旧的门店信息或桌位统计。 */
    public void evictStoresCache() {
        try {
            redis.delete(STORES_CACHE_KEY);
        } catch (Exception ignored) {
            // 缓存删除失败时等待 TTL 自然过期
        }
    }

    public List<Map<String, Object>> listTables(Long storeId, String date, String time, Integer partySize) {
        return listTables(storeId, date, time, partySize, null);
    }

    /** 桌位列表；availableOnly=true 时只返回 status=available 且该时段未被活跃预约占用的桌位。 */
    public List<Map<String, Object>> listTables(Long storeId, String date, String time, Integer partySize,
                                                Boolean availableOnly) {
        List<Map<String, Object>> rows = tableMapper.listTables(storeId,
                emptyToNull(date), emptyToNull(time), partySize);
        Normalizer.boolField(rows, "cat_zone");
        Normalizer.boolField(rows, "available_for_slot");
        if (Boolean.TRUE.equals(availableOnly)) {
            rows.removeIf(row -> !Boolean.TRUE.equals(row.get("available_for_slot")));
        }
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
