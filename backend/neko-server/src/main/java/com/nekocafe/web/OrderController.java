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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "点单与支付沙箱")
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
}
