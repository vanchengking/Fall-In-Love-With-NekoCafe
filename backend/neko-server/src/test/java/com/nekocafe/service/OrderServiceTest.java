package com.nekocafe.service;

import com.nekocafe.dto.OrderRequest;
import com.nekocafe.entity.OrderEntity;
import com.nekocafe.entity.PaymentTransaction;
import com.nekocafe.mapper.CatalogMapper;
import com.nekocafe.mapper.OrderItemMapper;
import com.nekocafe.mapper.OrderMapper;
import com.nekocafe.mapper.PaymentTransactionMapper;
import com.nekocafe.mapper.ReservationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private PaymentTransactionMapper paymentTransactionMapper;
    private CatalogMapper catalogMapper;
    private ReservationMapper reservationMapper;
    private UserService userService;
    private OrderService orderService;

    @BeforeEach
    void setup() {
        orderMapper = mock(OrderMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        paymentTransactionMapper = mock(PaymentTransactionMapper.class);
        catalogMapper = mock(CatalogMapper.class);
        reservationMapper = mock(ReservationMapper.class);
        userService = mock(UserService.class);
        orderService = new OrderService(
                orderMapper, orderItemMapper, paymentTransactionMapper, catalogMapper, reservationMapper, userService);
    }

    @Test
    @DisplayName("创建订单后 payment_status 应为 paid，且写入支付流水")
    void createSetsPaidPaymentStatus() {
        Map<String, Object> reservation = new HashMap<>();
        reservation.put("user_id", 8L);
        reservation.put("store_id", 3L);
        when(reservationMapper.getReservationDetail(11L)).thenReturn(reservation);
        when(catalogMapper.selectMenuByIds(List.of(101L))).thenReturn(List.of(Map.of("id", 101L, "price_cents", 1800L)));
        when(userService.getDiscountByUserId(8L)).thenReturn(1.0);
        doAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            order.setId(66L);
            return 1;
        }).when(orderMapper).insert(any(OrderEntity.class));

        Map<String, Object> result = orderService.create(new OrderRequest(11L, List.of(new OrderRequest.Item(101L, 2))));

        assertEquals("paid", result.get("payment_status"));
        assertEquals("paid", ((Map<?, ?>) result.get("payment")).get("status"));
        verify(paymentTransactionMapper, times(1)).insert(any(PaymentTransaction.class));
        verify(userService, times(1)).changePoints(eq(8L), eq(36), eq("order_paid"), eq(66L), any());
    }

    @Test
    @DisplayName("撤销已支付订单后应同步更新 payment_status=refunded 并写入退款流水")
    void cancelUpdatesRefundStatus() {
        OrderEntity order = new OrderEntity();
        order.setId(23L);
        order.setReservationId(9L);
        order.setUserId(7L);
        order.setStatus("paid");
        order.setPaymentStatus("paid");
        order.setTotalCents(2500);
        when(orderMapper.selectById(23L)).thenReturn(order);

        Map<String, Object> result = orderService.cancelOrder(23L);

        assertEquals("cancelled", result.get("status"));
        assertEquals("refunded", result.get("payment_status"));
        assertNotNull(result.get("refund"));
        verify(orderMapper, times(1)).updateById(order);
        verify(paymentTransactionMapper, times(1)).insert(any(PaymentTransaction.class));
        verify(userService, times(1)).changePoints(eq(7L), eq(-25), eq("order_cancelled"), eq(23L), any());
    }
}
