package com.nekocafe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 支付沙箱流水。
 */
@TableName("payment_transactions")
public class PaymentTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long reservationId;
    private Integer amountCents;
    private String channel;
    private String status;
    private String txnRef;

    public PaymentTransaction() {
    }

    public PaymentTransaction(Long orderId, Long reservationId, Integer amountCents,
                              String channel, String status, String txnRef) {
        this.orderId = orderId;
        this.reservationId = reservationId;
        this.amountCents = amountCents;
        this.channel = channel;
        this.status = status;
        this.txnRef = txnRef;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }

    public Integer getAmountCents() { return amountCents; }
    public void setAmountCents(Integer amountCents) { this.amountCents = amountCents; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTxnRef() { return txnRef; }
    public void setTxnRef(String txnRef) { this.txnRef = txnRef; }
}
