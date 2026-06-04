package com.nekocafe.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nekocafe.common.ApiResponse;
import com.nekocafe.common.Payloads;
import com.nekocafe.dto.CatHealthRequest;
import com.nekocafe.mapper.CatHealthRecordMapper;
import com.nekocafe.mapper.CatalogMapper;
import com.nekocafe.service.CatService;
import com.nekocafe.service.CatalogService;
import com.nekocafe.service.DashboardService;
import com.nekocafe.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Catalog", description = "门店 / 桌位 / 菜品 / 猫咪 / 推荐 / 看板")
public class CatalogController {

    private final CatalogService catalogService;
    private final CatService catService;
    private final RecommendationService recommendationService;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;
    private final CatalogMapper catalogMapper;

    public CatalogController(CatalogService catalogService,
                            CatService catService,
                            RecommendationService recommendationService,
                            DashboardService dashboardService,
                            ObjectMapper objectMapper,
                            CatalogMapper catalogMapper) {
        this.catalogService = catalogService;
        this.catService = catService;
        this.recommendationService = recommendationService;
        this.dashboardService = dashboardService;
        this.objectMapper = objectMapper;
        this.catalogMapper = catalogMapper;
    }

    @Operation(summary = "门店列表（含桌位数/座位数，Redis 缓存）")
    @GetMapping("/stores")
    public ApiResponse stores() {
        return ApiResponse.of(catalogService.listStores());
    }

    @Operation(summary = "桌位列表与时段可用性")
    @GetMapping("/tables")
    public ApiResponse tables(@RequestParam(required = false) Long storeId,
                              @RequestParam(required = false) String date,
                              @RequestParam(required = false) String time,
                              @RequestParam(required = false) Integer partySize) {
        return ApiResponse.of(catalogService.listTables(storeId, date, time, partySize));
    }

    @Operation(summary = "菜品列表")
    @GetMapping("/menu-items")
    public ApiResponse menuItems(@RequestParam(required = false) Long storeId) {
        return ApiResponse.of(catalogService.listMenuItems(storeId));
    }

    @Operation(summary = "猫咪档案列表")
    @GetMapping("/cats")
    public ApiResponse cats(@RequestParam(required = false) Long storeId) {
        return ApiResponse.of(catalogService.listCats(storeId));
    }

    @Operation(summary = "猫咪健康记录列表")
    @GetMapping("/cat-health-records")
    public ApiResponse catHealthRecords(@RequestParam(required = false) Long catId) {
        return ApiResponse.of(catalogService.listCatHealthRecords(catId));
    }

    @Operation(summary = "新增猫咪健康记录（cat_keeper/manager/admin）")
    @PostMapping("/cat-health-records")
    public ResponseEntity<ApiResponse> addCatHealthRecord(@RequestBody Map<String, Object> body) {
        CatHealthRequest req = objectMapper.convertValue(Payloads.unwrap(body), CatHealthRequest.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(catService.addHealthRecord(req)));
    }

    @Operation(summary = "猫咪疫苗记录列表")
    @GetMapping("/vaccine-records")
    public ApiResponse vaccineRecords(@RequestParam(required = false) Long catId) {
        return ApiResponse.of(catalogService.listVaccineRecords(catId));
    }

    @Operation(summary = "规则型推荐（人-桌-菜-猫匹配）")
    @GetMapping("/recommendations")
    public ApiResponse recommendations(@RequestParam(required = false) Long userId,
                                       @RequestParam(required = false) Long storeId,
                                       @RequestParam(required = false) String preferences) {
        List<String> prefs = new ArrayList<>();
        if (preferences != null && !preferences.isBlank()) {
            for (String tag : preferences.split(",")) {
                if (!tag.trim().isEmpty()) {
                    prefs.add(tag.trim());
                }
            }
        }
        return ApiResponse.of(recommendationService.recommend(userId, storeId, prefs));
    }

    @Operation(summary = "运营看板汇总")
    @GetMapping("/dashboard/summary")
    public ApiResponse dashboardSummary(@RequestParam(required = false) Long storeId) {
        return ApiResponse.of(dashboardService.summary(storeId));
    }

    @Operation(summary = "更新猫咪照片")
    @PatchMapping("/cats/{id}/photo")
    public ApiResponse updateCatPhoto(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String photoUrl = body.get("photoUrl");
        catalogMapper.updateCatPhoto(id, photoUrl);
        return ApiResponse.of(Map.of("id", id, "photo_url", photoUrl));
    }

    @Operation(summary = "更新菜品照片")
    @PatchMapping("/menu-items/{id}/photo")
    public ApiResponse updateMenuItemPhoto(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String photoUrl = body.get("photoUrl");
        catalogMapper.updateMenuItemPhoto(id, photoUrl);
        return ApiResponse.of(Map.of("id", id, "photo_url", photoUrl));
    }
}
