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
import com.nekocafe.mapper.ReservationMapper;
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
    private static final String ORDER_STATUS_PAID = "paid";
    private static final String ORDER_STATUS_CANCELLED = "cancelled";
    private static final String PAYMENT_STATUS_PAID = "paid";
    private static final String PAYMENT_STATUS_REFUNDED = "refunded";
    private static final String LEGACY_PAYMENT_STATUS_SANDBOX_PAID = "sandbox_paid";

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final CatalogMapper catalogMapper;
    private final ReservationMapper reservationMapper;
    private final UserService userService;

    public OrderService(OrderMapper orderMapper,
                        OrderItemMapper orderItemMapper,
                        PaymentTransactionMapper paymentTransactionMapper,
                        CatalogMapper catalogMapper,
                        ReservationMapper reservationMapper,
                        UserService userService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentTransactionMapper = paymentTransactionMapper;
        this.catalogMapper = catalogMapper;
        this.reservationMapper = reservationMapper;
        this.userService = userService;
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

        Map<String, Object> reservation = reservationMapper.getReservationDetail(request.reservationId());
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

        // 应用会员折扣
        double discountRate = userService.getDiscountByUserId(userId);
        long discountedTotal = Math.round(total * discountRate);

        OrderEntity order = new OrderEntity();
        order.setReservationId(request.reservationId());
        order.setUserId(userId);
        order.setStoreId(storeId);
        order.setStatus(ORDER_STATUS_PAID);
        order.setPaymentStatus(PAYMENT_STATUS_PAID);
        order.setOriginalTotalCents((int) total);  // 保存原价
        order.setDiscountRate(discountRate);         // 保存折扣率
        order.setTotalCents((int) discountedTotal); // 保存折扣后价格
        orderMapper.insert(order);

        for (OrderRequest.Item item : request.items()) {
            int quantity = Math.max(1, item.quantity() == null ? 1 : item.quantity());
            orderItemMapper.insert(new OrderItem(
                    order.getId(), item.menuItemId(), quantity, priceById.get(item.menuItemId()).intValue()));
        }

        String txnRef = "SBX-" + order.getId();
        paymentTransactionMapper.insert(new PaymentTransaction(
                order.getId(), request.reservationId(), (int) discountedTotal, "sandbox", "paid", txnRef));

        // 支付成功后增加积分（按折后价，1元=1积分），同步落积分流水
        // 若积分增加失败，抛出异常触发事务回滚，订单不会创建
        try {
            int pointsToAdd = Math.toIntExact(discountedTotal / 100);
            userService.changePoints(userId, pointsToAdd, "order_paid", order.getId(), "订单支付积分（1元=1积分）");
        } catch (Exception e) {
            throw ApiException.badRequest("积分增加失败：" + e.getMessage());
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", order.getId());
        out.put("reservation_id", order.getReservationId());
        out.put("user_id", order.getUserId());
        out.put("store_id", order.getStoreId());
        out.put("status", order.getStatus());
        out.put("payment_status", order.getPaymentStatus());
        out.put("original_total_cents", order.getOriginalTotalCents());  // 原价
        out.put("discount_rate", order.getDiscountRate());                // 折扣率
        out.put("total_cents", order.getTotalCents());                  // 折后价
        out.put("payment", Map.of("txn_ref", txnRef, "channel", "sandbox", "status", PAYMENT_STATUS_PAID));
        return out;
    }

    /**
     * 取消订单并返还积分
     */
    @Transactional
    public Map<String, Object> cancelOrder(Long orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw ApiException.notFound("订单不存在");
        }
        if (!ORDER_STATUS_PAID.equals(order.getStatus())) {
            throw ApiException.badRequest("只能撤销已支付的订单");
        }
        if (!isPaidPaymentStatus(order.getPaymentStatus())) {
            throw ApiException.badRequest("当前订单未处于已支付状态，无法触发退款");
        }

        // 先扣回支付时发放的积分（如果失败，事务回滚，订单状态不会更新）
        int pointsToDeduct = order.getTotalCents() / 100;
        try {
            userService.changePoints(order.getUserId(), -pointsToDeduct, "order_cancelled", order.getId(), "订单撤销，扣回支付积分");
        } catch (Exception e) {
            // 积分扣回失败，抛出异常触发事务回滚
            throw ApiException.badRequest("积分返还失败：" + e.getMessage());
        }

        // 积分返还成功后，再更新订单状态
        order.setStatus(ORDER_STATUS_CANCELLED);
        order.setPaymentStatus(PAYMENT_STATUS_REFUNDED);
        orderMapper.updateById(order);
        paymentTransactionMapper.insert(new PaymentTransaction(
                order.getId(), order.getReservationId(), order.getTotalCents(),
                "sandbox", PAYMENT_STATUS_REFUNDED, "SBX-REFUND-" + order.getId()));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", order.getId());
        out.put("status", order.getStatus());
        out.put("payment_status", order.getPaymentStatus());
        out.put("total_cents", order.getTotalCents());
        out.put("refund", Map.of("channel", "sandbox", "status", PAYMENT_STATUS_REFUNDED));
        return out;
    }

    public Map<String, Object> getOrderDetail(Long orderId) {
        Map<String, Object> order = catalogMapper.getOrderDetail(orderId);
        if (order == null) {
            throw ApiException.notFound("订单不存在");
        }
        List<Map<String, Object>> items = catalogMapper.getOrderItems(orderId);
        order.put("items", items);
        return order;
    }

    private static boolean isPaidPaymentStatus(String paymentStatus) {
        return PAYMENT_STATUS_PAID.equals(paymentStatus)
                || LEGACY_PAYMENT_STATUS_SANDBOX_PAID.equals(paymentStatus);
    }
}
