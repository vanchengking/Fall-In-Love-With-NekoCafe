package com.nekocafe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nekocafe.mapper.CatalogMapper;
import com.nekocafe.mapper.StoreMapper;
import com.nekocafe.mapper.TableMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 门店列表 Redis 缓存读写/失效与桌位布尔字段归一化（M2 公开目录服务）。
 */
@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    private static final String CACHE_KEY = "neko:cache:stores";

    @Mock
    private CatalogMapper catalogMapper;
    @Mock
    private StoreMapper storeMapper;
    @Mock
    private TableMapper tableMapper;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        catalogService = new CatalogService(catalogMapper, storeMapper, tableMapper,
                redis, new ObjectMapper(), 60);
    }

    @Test
    @DisplayName("缓存未命中：回源数据库并写入 60s 缓存")
    void listStoresFallsBackToDbAndCaches() {
        when(valueOps.get(CACHE_KEY)).thenReturn(null);
        Map<String, Object> store = new HashMap<>();
        store.put("id", 1);
        store.put("city", "北京");
        when(storeMapper.listStores()).thenReturn(List.of(store));

        List<Map<String, Object>> result = catalogService.listStores();

        assertEquals(1, result.size());
        assertEquals("北京", result.get(0).get("city"));
        verify(valueOps).set(eq(CACHE_KEY), anyString(), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("缓存命中：直接返回缓存，不查数据库")
    void listStoresUsesCacheWhenPresent() {
        when(valueOps.get(CACHE_KEY)).thenReturn("[{\"id\":1,\"city\":\"上海\"}]");

        List<Map<String, Object>> result = catalogService.listStores();

        assertEquals("上海", result.get(0).get("city"));
        verify(storeMapper, never()).listStores();
    }

    @Test
    @DisplayName("evictStoresCache：删除门店缓存 key")
    void evictStoresCacheDeletesKey() {
        catalogService.evictStoresCache();
        verify(redis).delete(CACHE_KEY);
    }

    @Test
    @DisplayName("listTables：cat_zone/available_for_slot 归一化为 boolean")
    void listTablesNormalizesBooleanFields() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        row.put("seats", 4);
        row.put("area", "window");
        row.put("cat_zone", 1);
        row.put("available_for_slot", 0);
        when(tableMapper.listTables(anyLong(), any(), any(), any())).thenReturn(List.of(row));

        List<Map<String, Object>> result = catalogService.listTables(1L, "", "", 2);

        assertEquals(Boolean.TRUE, result.get(0).get("cat_zone"));
        assertEquals(Boolean.FALSE, result.get(0).get("available_for_slot"));
        verify(tableMapper).listTables(1L, null, null, 2);
    }

    @Test
    @DisplayName("listTables：只传一个时段字段时，空字符串归一化为 null 后透传 Mapper")
    void listTablesNormalizesSingleBlankSlotFieldToNull() {
        when(tableMapper.listTables(anyLong(), any(), any(), any())).thenReturn(List.of());

        catalogService.listTables(1L, "2026-09-01", "", 2);

        verify(tableMapper).listTables(1L, "2026-09-01", null, 2);
    }

    @Test
    @DisplayName("listTables：availableOnly=true 只保留 available_for_slot=true 的桌位")
    void listTablesAvailableOnlyFiltersOccupiedAndDisabledRows() {
        Map<String, Object> free = new HashMap<>();
        free.put("id", 1);
        free.put("status", "available");
        free.put("cat_zone", 0);
        free.put("available_for_slot", 1);
        Map<String, Object> occupied = new HashMap<>();
        occupied.put("id", 2);
        occupied.put("status", "available");
        occupied.put("cat_zone", 0);
        occupied.put("available_for_slot", 0);
        Map<String, Object> maintenance = new HashMap<>();
        maintenance.put("id", 3);
        maintenance.put("status", "maintenance");
        maintenance.put("cat_zone", 0);
        maintenance.put("available_for_slot", 0);
        when(tableMapper.listTables(anyLong(), anyString(), anyString(), any()))
                .thenReturn(new ArrayList<>(List.of(free, occupied, maintenance)));

        List<Map<String, Object>> result =
                catalogService.listTables(1L, "2026-09-01", "18:30", 2, true);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("id"));
        assertEquals(Boolean.TRUE, result.get(0).get("available_for_slot"));
    }

    @Test
    @DisplayName("listTables：不传 availableOnly 时保留不可用桌位（available_for_slot=false）")
    void listTablesWithoutAvailableOnlyKeepsUnavailableRows() {
        Map<String, Object> occupied = new HashMap<>();
        occupied.put("id", 2);
        occupied.put("cat_zone", 0);
        occupied.put("available_for_slot", 0);
        when(tableMapper.listTables(anyLong(), anyString(), anyString(), any()))
                .thenReturn(new ArrayList<>(List.of(occupied)));

        List<Map<String, Object>> result =
                catalogService.listTables(1L, "2026-09-01", "18:30", 2, null);

        assertEquals(1, result.size());
        assertEquals(Boolean.FALSE, result.get(0).get("available_for_slot"));
    }
}
