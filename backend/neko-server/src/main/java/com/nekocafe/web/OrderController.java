package com.nekocafe.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nekocafe.common.ApiResponse;
import com.nekocafe.common.Payloads;
import com.nekocafe.dto.OrderRequest;
import com.nekocafe.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "点单与支付")
public class OrderController {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public OrderController(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "订单列表")
    @GetMapping
    public ApiResponse list(@RequestParam(required = false) Long storeId) {
        return ApiResponse.of(orderService.list(storeId));
    }

    @Operation(summary = "创建订单并完成支付沙箱（写入 payment_transactions）")
    @PostMapping
    public ResponseEntity<ApiResponse> create(@RequestBody Map<String, Object> body) {
        OrderRequest req = objectMapper.convertValue(Payloads.unwrap(body), OrderRequest.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(orderService.create(req)));
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    public ApiResponse getDetail(@PathVariable Long id) {
        return ApiResponse.of(orderService.getOrderDetail(id));
    }

    @Operation(summary = "取消订单（撤销并返还积分）")
    @PatchMapping("/{id}/cancel")
    public ApiResponse cancelOrder(@PathVariable Long id) {
        return ApiResponse.of(orderService.cancelOrder(id));
    }
}
