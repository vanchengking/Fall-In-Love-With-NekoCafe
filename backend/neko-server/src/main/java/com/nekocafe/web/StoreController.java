package com.nekocafe.web;

import com.nekocafe.common.ApiResponse;
import com.nekocafe.service.StoreAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/stores")
@Tag(name = "Store Admin", description = "Store management queries")
public class StoreController {

    private final StoreAdminService storeAdminService;

    public StoreController(StoreAdminService storeAdminService) {
        this.storeAdminService = storeAdminService;
    }

    @Operation(summary = "List stores")
    @GetMapping
    public ApiResponse listStores() {
        return ApiResponse.of(storeAdminService.listStores());
    }

    @Operation(summary = "Get store detail")
    @GetMapping("/{id}")
    public ApiResponse getStoreDetail(@PathVariable Long id) {
        return ApiResponse.of(storeAdminService.getStoreDetail(id));
    }

    @Operation(summary = "Create store")
    @PostMapping
    public ResponseEntity<ApiResponse> createStore(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(storeAdminService.createStore(body)));
    }

    @Operation(summary = "Update store")
    @PutMapping("/{id}")
    public ApiResponse updateStore(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.of(storeAdminService.updateStore(id, body));
    }

    @Operation(summary = "Delete store")
    @DeleteMapping("/{id}")
    public ApiResponse deleteStore(@PathVariable Long id) {
        return ApiResponse.of(storeAdminService.deleteStore(id));
    }
}
