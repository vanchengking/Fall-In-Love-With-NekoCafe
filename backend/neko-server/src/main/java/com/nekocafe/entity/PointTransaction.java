package com.nekocafe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 积分流水：记录每次积分变更的 delta 与变更后余额。
 * (source_type, source_id) 数据库唯一约束防止同一来源重复发放（source_id 为 NULL 时不受限）。
 */
@TableName("point_transactions")
public class PointTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer delta;
    private Integer balanceAfter;
    private String sourceType;
    private Long sourceId;
    private String reason;
    private String createdAt;

    public PointTransaction() {
    }

    public PointTransaction(Long userId, Integer delta, Integer balanceAfter,
                            String sourceType, Long sourceId, String reason) {
        this.userId = userId;
        this.delta = delta;
        this.balanceAfter = balanceAfter;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getDelta() { return delta; }
    public void setDelta(Integer delta) { this.delta = delta; }

    public Integer getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
