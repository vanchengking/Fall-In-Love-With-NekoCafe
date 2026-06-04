package com.nekocafe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("order_items")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long menuItemId;
    private Integer quantity;
    private Integer unitPriceCents;

    public OrderItem() {
    }

    public OrderItem(Long orderId, Long menuItemId, Integer quantity, Integer unitPriceCents) {
        this.orderId = orderId;
        this.menuItemId = menuItemId;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getMenuItemId() { return menuItemId; }
    public void setMenuItemId(Long menuItemId) { this.menuItemId = menuItemId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getUnitPriceCents() { return unitPriceCents; }
    public void setUnitPriceCents(Integer unitPriceCents) { this.unitPriceCents = unitPriceCents; }
}
