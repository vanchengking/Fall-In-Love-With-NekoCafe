package com.nekocafe.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nekocafe.common.ApiResponse;
import com.nekocafe.common.Payloads;
import com.nekocafe.dto.ReservationRequest;
import com.nekocafe.dto.ReservationRescheduleRequest;
import com.nekocafe.security.AuthUser;
import com.nekocafe.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "预约创建、查询与状态机流转")
public class ReservationController {

    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;

    public ReservationController(ReservationService reservationService, ObjectMapper objectMapper) {
        this.reservationService = reservationService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "预约列表（可按日期/手机号/门店/状态筛选）")
    @GetMapping
    public ApiResponse list(@RequestParam(required = false) String date,
                            @RequestParam(required = false) String mobileNumber,
                            @RequestParam(required = false) Long storeId,
                            @RequestParam(required = false) String status) {
        return ApiResponse.of(reservationService.list(date, mobileNumber, storeId, status));
    }

    @Operation(summary = "创建预约（事务 + 桌位行锁 + 双重唯一约束防重复）")
    @PostMapping
    public ResponseEntity<ApiResponse> create(@RequestBody Map<String, Object> body) {
        ReservationRequest req = objectMapper.convertValue(Payloads.unwrap(body), ReservationRequest.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(reservationService.create(req)));
    }

    @Operation(summary = "预约状态流转（staff/manager/operator/admin），严格状态机校验")
    @PatchMapping("/{id}/status")
    public ApiResponse updateStatus(@PathVariable Long id,
                                    @RequestBody Map<String, Object> body,
                                    @AuthenticationPrincipal AuthUser actor) {
        Object status = Payloads.unwrap(body).get("status");
        return ApiResponse.of(reservationService.updateStatus(id, status == null ? null : String.valueOf(status), actor));
    }

    @Operation(summary = "取消预约：顾客取消本人 created/booked 预约，后台角色可代客取消")
    @PatchMapping("/{id}/cancel")
    public ApiResponse cancel(@PathVariable Long id,
                              @AuthenticationPrincipal AuthUser actor) {
        return ApiResponse.of(reservationService.cancel(id, actor));
    }

    @PatchMapping("/{id}/reschedule")
    @Operation(summary = "改约预约：更新时间、人数和桌位，释放原桌位时段占用")
    public ApiResponse reschedule(@PathVariable Long id,
                                  @RequestBody Map<String, Object> body,
                                  @AuthenticationPrincipal AuthUser actor) {
        ReservationRescheduleRequest req =
                objectMapper.convertValue(Payloads.unwrap(body), ReservationRescheduleRequest.class);
        return ApiResponse.of(reservationService.reschedule(id, req, actor));
    }

    @Operation(summary = "预约状态变更事件记录")
    @GetMapping("/{id}/events")
    public ApiResponse events(@PathVariable Long id) {
        return ApiResponse.of(reservationService.events(id));
    }
}
