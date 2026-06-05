package com.nekocafe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("coupons")
public class Coupon {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String title;
    private Integer discountCents;
    private Integer minSpendCents;
    private String validFrom;
    private String validTo;
    private String status;
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getDiscountCents() { return discountCents; }
    public void setDiscountCents(Integer discountCents) { this.discountCents = discountCents; }

    public Integer getMinSpendCents() { return minSpendCents; }
    public void setMinSpendCents(Integer minSpendCents) { this.minSpendCents = minSpendCents; }

    public String getValidFrom() { return validFrom; }
    public void setValidFrom(String validFrom) { this.validFrom = validFrom; }

    public String getValidTo() { return validTo; }
    public void setValidTo(String validTo) { this.validTo = validTo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
