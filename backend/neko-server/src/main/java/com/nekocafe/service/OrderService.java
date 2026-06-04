package com.nekocafe.service;

import com.nekocafe.common.ApiException;
import com.nekocafe.common.Normalizer;
import com.nekocafe.dto.OrderRequest;
import com.nekocafe.entity.OrderEntity;
import com.nekocafe.entity.OrderItem;
import com.nekocafe.entity.PaymentTransaction;
import com.nekocafe.mapper.CatalogMapper;
import com.nekocafe.mapper.OrderItemMapper;
import com.nekocafe.mapper.OrderMapper;
import com.nekocafe.mapper.PaymentTransactionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 点单 + 支付沙箱：生成订单与明细，并写入 payment_transactions 流水。
 */
@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final CatalogMapper catalogMapper;

    public OrderService(OrderMapper orderMapper,
                        OrderItemMapper orderItemMapper,
                        PaymentTransactionMapper paymentTransactionMapper,
                        CatalogMapper catalogMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentTransactionMapper = paymentTransactionMapper;
        this.catalogMapper = catalogMapper;
    }

    public List<Map<String, Object>> list(Long storeId) {
        return catalogMapper.listOrders(storeId);
    }

    @Transactional
    public Map<String, Object> create(OrderRequest request) {
        if (request == null || request.reservationId() == null
                || request.items() == null || request.items().isEmpty()) {
            throw ApiException.badRequest("reservationId and items are required");
        }

        Map<String, Object> reservation = catalogMapper.getReservationDetail(request.reservationId());
        if (reservation == null) {
            throw ApiException.notFound("reservation " + request.reservationId() + " not found");
        }

        List<Long> menuIds = request.items().stream()
                .map(OrderRequest.Item::menuItemId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (menuIds.isEmpty()) {
            throw ApiException.badRequest("at least one valid menuItemId is required");
        }

        Map<Long, Long> priceById = new HashMap<>();
        for (Map<String, Object> row : catalogMapper.selectMenuByIds(menuIds)) {
            priceById.put(Normalizer.toLong(row.get("id")), Normalizer.toLong(row.get("price_cents")));
        }

        long total = 0;
        for (OrderRequest.Item item : request.items()) {
            Long price = priceById.get(item.menuItemId());
            if (price == null) {
                throw ApiException.badRequest("menu item " + item.menuItemId() + " not found");
            }
            int quantity = Math.max(1, item.quantity() == null ? 1 : item.quantity());
            total += price * quantity;
        }

        Long userId = Normalizer.toLong(reservation.get("user_id"));
        Long storeId = Normalizer.toLong(reservation.get("store_id"));

        OrderEntity order = new OrderEntity();
        order.setReservationId(request.reservationId());
        order.setUserId(userId);
        order.setStoreId(storeId);
        order.setStatus("paid");
        order.setPaymentStatus("sandbox_paid");
        order.setTotalCents((int) total);
        orderMapper.insert(order);

        for (OrderRequest.Item item : request.items()) {
            int quantity = Math.max(1, item.quantity() == null ? 1 : item.quantity());
            orderItemMapper.insert(new OrderItem(
                    order.getId(), item.menuItemId(), quantity, priceById.get(item.menuItemId()).intValue()));
        }

        String txnRef = "SBX-" + order.getId();
        paymentTransactionMapper.insert(new PaymentTransaction(
                order.getId(), request.reservationId(), (int) total, "sandbox", "paid", txnRef));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", order.getId());
        out.put("reservation_id", order.getReservationId());
        out.put("user_id", order.getUserId());
        out.put("store_id", order.getStoreId());
        out.put("status", order.getStatus());
        out.put("payment_status", order.getPaymentStatus());
        out.put("total_cents", order.getTotalCents());
        out.put("payment", Map.of("txn_ref", txnRef, "channel", "sandbox", "status", "paid"));
        return out;
    }
}
