package com.nekocafe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("reviews")
public class ReviewEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long storeId;
    private Long orderId;       // 关联订单

    // 各维度评分 1-5
    private Integer rating;          // 综合评分（必填）
    private Integer foodRating;      // 菜品评分
    private Integer serviceRating;   // 服务评分
    private Integer environmentRating; // 环境评分
    private Integer catRating;       // 猫咪互动评分

    private String content;          // 评价内容
    private Boolean isAnonymous;     // 是否匿名

    private String reply;           // 商家回复
    private LocalDateTime repliedAt; // 回复时间

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ===== Getter & Setter =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public Integer getFoodRating() { return foodRating; }
    public void setFoodRating(Integer foodRating) { this.foodRating = foodRating; }

    public Integer getServiceRating() { return serviceRating; }
    public void setServiceRating(Integer serviceRating) { this.serviceRating = serviceRating; }

    public Integer getEnvironmentRating() { return environmentRating; }
    public void setEnvironmentRating(Integer environmentRating) { this.environmentRating = environmentRating; }

    public Integer getCatRating() { return catRating; }
    public void setCatRating(Integer catRating) { this.catRating = catRating; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Boolean getIsAnonymous() { return isAnonymous; }
    public void setIsAnonymous(Boolean anonymous) { isAnonymous = anonymous; }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public LocalDateTime getRepliedAt() { return repliedAt; }
    public void setRepliedAt(LocalDateTime repliedAt) { this.repliedAt = repliedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
