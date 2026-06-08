package com.nekocafe.web;

import com.nekocafe.common.ApiResponse;
import com.nekocafe.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Dashboard Admin", description = "Dashboard management queries")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Get dashboard summary")
    @GetMapping("/summary")
    public ApiResponse summary(@RequestParam(required = false) Long storeId) {
        return ApiResponse.of(dashboardService.summary(storeId));
    }

    @Operation(summary = "List dashboard alerts")
    @GetMapping("/alerts")
    public ApiResponse alerts(@RequestParam(required = false) Long storeId) {
        return ApiResponse.of(dashboardService.alerts(storeId));
    }

    @Operation(summary = "Get reservation trend")
    @GetMapping("/trends/reservations")
    public ApiResponse reservationTrend(@RequestParam(required = false) Long storeId,
                                        @RequestParam(required = false) Integer days) {
        return ApiResponse.of(dashboardService.reservationTrend(storeId, days));
    }

    @Operation(summary = "Get revenue trend")
    @GetMapping("/trends/revenue")
    public ApiResponse revenueTrend(@RequestParam(required = false) Long storeId,
                                    @RequestParam(required = false) Integer days) {
        return ApiResponse.of(dashboardService.revenueTrend(storeId, days));
    }

    @Operation(summary = "Get store overview")
    @GetMapping("/stores-overview")
    public ApiResponse storeOverview(@RequestParam(required = false) Long storeId) {
        return ApiResponse.of(dashboardService.storeOverview(storeId));
    }
}
