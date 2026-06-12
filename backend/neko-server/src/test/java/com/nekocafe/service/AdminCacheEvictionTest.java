package com.nekocafe.service;

import com.nekocafe.mapper.StoreMapper;
import com.nekocafe.mapper.TableMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 门店/桌位管理端 CRUD 后必须失效 Redis 门店缓存（neko:cache:stores），
 * 否则 TTL 内 GET /api/stores 会返回旧的门店信息或桌位统计。
 */
@ExtendWith(MockitoExtension.class)
class AdminCacheEvictionTest {

    @Mock
    private StoreMapper storeMapper;
    @Mock
    private TableMapper tableMapper;
    @Mock
    private CatalogService catalogService;

    private StoreAdminService storeAdminService;
    private TableAdminService tableAdminService;

    @BeforeEach
    void setUp() {
        storeAdminService = new StoreAdminService(storeMapper, catalogService);
        tableAdminService = new TableAdminService(tableMapper, catalogService);
    }

    private Map<String, Object> storeBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "NekoCafe 测试店");
        body.put("city", "北京");
        body.put("address", "测试路 1 号");
        body.put("phone", "010-00000000");
        body.put("openTime", "10:00");
        body.put("closeTime", "22:00");
        return body;
    }

    private Map<String, Object> tableBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("storeId", 1);
        body.put("code", "T01");
        body.put("seats", 4);
        body.put("area", "main");
        return body;
    }

    @Test
    @DisplayName("新增门店后清除门店缓存")
    void createStoreEvictsCache() {
        when(storeMapper.insertStore(any())).thenAnswer(inv -> {
            Map<String, Object> store = inv.getArgument(0);
            store.put("id", 99L);
            return 1;
        });
        when(storeMapper.getStoreDetail(99L)).thenReturn(new HashMap<>(Map.of("id", 99L)));

        storeAdminService.createStore(storeBody());

        verify(catalogService).evictStoresCache();
    }

    @Test
    @DisplayName("更新/删除门店后清除门店缓存")
    void updateAndDeleteStoreEvictCache() {
        when(storeMapper.getStoreDetail(anyLong())).thenReturn(new HashMap<>(Map.of("id", 1L)));
        when(storeMapper.updateStore(any())).thenReturn(1);
        when(storeMapper.deleteStore(1L)).thenReturn(1);

        storeAdminService.updateStore(1L, storeBody());
        storeAdminService.deleteStore(1L);

        verify(catalogService, org.mockito.Mockito.times(2)).evictStoresCache();
    }

    @Test
    @DisplayName("新增桌位后清除门店缓存（table_count/total_seats 统计变化）")
    void createTableEvictsCache() {
        when(tableMapper.insertTable(any())).thenAnswer(inv -> {
            Map<String, Object> table = inv.getArgument(0);
            table.put("id", 88L);
            return 1;
        });
        when(tableMapper.getTableDetail(eq(88L), isNull(), isNull()))
                .thenReturn(new HashMap<>(Map.of("id", 88L, "cat_zone", 0)));

        tableAdminService.createTable(tableBody());

        verify(catalogService).evictStoresCache();
    }

    @Test
    @DisplayName("更新/删除桌位后清除门店缓存")
    void updateAndDeleteTableEvictCache() {
        when(tableMapper.getTableDetail(anyLong(), isNull(), isNull()))
                .thenReturn(new HashMap<>(Map.of("id", 1L, "cat_zone", 1)));
        when(tableMapper.updateTable(any())).thenReturn(1);
        when(tableMapper.deleteTable(1L)).thenReturn(1);

        tableAdminService.updateTable(1L, tableBody());
        tableAdminService.deleteTable(1L);

        verify(catalogService, org.mockito.Mockito.times(2)).evictStoresCache();
    }
}
