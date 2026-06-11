package com.nekocafe.web;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nekocafe.common.Payloads;
import com.nekocafe.entity.ReviewEntity;
import com.nekocafe.mapper.ReviewMapper;
import com.nekocafe.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Tag(name = "评价管理")
@RequestMapping("/api/reviews")
@RestController
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewMapper reviewMapper;
    private final ObjectMapper objectMapper;

    public ReviewController(ReviewMapper reviewMapper, ObjectMapper objectMapper) {
        this.reviewMapper = reviewMapper;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "提交评价")
    @PostMapping
    public ReviewEntity create(@AuthenticationPrincipal AuthUser user,
                               @RequestBody Map<String, Object> body) {
        try {
            ReviewRequest req = objectMapper.convertValue(Payloads.unwrap(body), ReviewRequest.class);
            log.info("Creating review: userId={}, storeId={}, rating={}", user == null ? null : user.id(), req.storeId(), req.rating());
            if (user == null) {
                throw new RuntimeException("用户未登录");
            }
            if (req.storeId() == null) {
                throw new RuntimeException("storeId is required");
            }
            if (req.rating() == null || req.rating() < 1 || req.rating() > 5) {
                throw new RuntimeException("rating must be between 1 and 5");
            }

            ReviewEntity review = new ReviewEntity();
            review.setUserId(user.id());
            review.setStoreId(req.storeId());
            review.setOrderId(req.orderId());
            review.setRating(req.rating());
            review.setContent(req.content());
            review.setFoodRating(req.foodRating());
            review.setServiceRating(req.serviceRating());
            review.setEnvironmentRating(req.environmentRating());
            review.setCatRating(req.catRating());
            review.setIsAnonymous(req.isAnonymous());
            review.setCreatedAt(LocalDateTime.now());
            review.setUpdatedAt(LocalDateTime.now());

            log.info("Inserting review: {}", review);
            reviewMapper.insert(review);
            log.info("Review created with id: {}", review.getId());
            return review;
        } catch (Exception e) {
            log.error("Error creating review", e);
            throw new RuntimeException("提交评价失败: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "修改我的评价")
    @PutMapping("/{id}")
    public ReviewEntity update(@AuthenticationPrincipal AuthUser user,
                               @PathVariable Long id,
                               @RequestBody Map<String, Object> body) {
        try {
            if (user == null) {
                throw new RuntimeException("用户未登录");
            }
            ReviewEntity existing = reviewMapper.selectById(id);
            if (existing == null) {
                throw new RuntimeException("评价不存在");
            }
            if (!existing.getUserId().equals(user.id())) {
                throw new RuntimeException("无权修改此评价");
            }
            // 已回复的评价不允许修改
            if (existing.getReply() != null && !existing.getReply().isEmpty()) {
                throw new RuntimeException("商家已回复，无法修改");
            }

            ReviewRequest req = objectMapper.convertValue(Payloads.unwrap(body), ReviewRequest.class);

            if (req.rating() != null) {
                if (req.rating() < 1 || req.rating() > 5) {
                    throw new RuntimeException("rating must be between 1 and 5");
                }
                existing.setRating(req.rating());
            }
            if (req.content() != null) existing.setContent(req.content());
            if (req.foodRating() != null) existing.setFoodRating(req.foodRating());
            if (req.serviceRating() != null) existing.setServiceRating(req.serviceRating());
            if (req.environmentRating() != null) existing.setEnvironmentRating(req.environmentRating());
            if (req.catRating() != null) existing.setCatRating(req.catRating());
            if (req.isAnonymous() != null) existing.setIsAnonymous(req.isAnonymous());
            existing.setUpdatedAt(LocalDateTime.now());

            reviewMapper.updateById(existing);
            log.info("Review updated: id={}", id);
            return existing;
        } catch (Exception e) {
            log.error("Error updating review", e);
            throw new RuntimeException("修改评价失败: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "商家回复评价（店员/管理员）")
    @PostMapping("/{id}/reply")
    public ReviewEntity reply(@AuthenticationPrincipal AuthUser user,
                              @PathVariable Long id,
                              @RequestBody Map<String, String> body) {
        try {
            if (user == null) {
                throw new RuntimeException("用户未登录");
            }
            String replyText = body.get("reply");
            if (replyText == null || replyText.isBlank()) {
                throw new RuntimeException("reply content is required");
            }

            ReviewEntity existing = reviewMapper.selectById(id);
            if (existing == null) {
                throw new RuntimeException("评价不存在");
            }

            existing.setReply(replyText);
            existing.setRepliedAt(LocalDateTime.now());
            existing.setUpdatedAt(LocalDateTime.now());

            reviewMapper.updateById(existing);
            log.info("Review replied: id={}, reply={}", id, replyText);
            return existing;
        } catch (Exception e) {
            log.error("Error replying to review", e);
            throw new RuntimeException("回复失败: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "查看门店评价")
    @GetMapping("/store/{storeId}")
    public List<Map<String, Object>> getByStore(@PathVariable Long storeId,
                                                @RequestParam(defaultValue = "20") Integer limit) {
        try {
            log.info("Getting reviews for storeId={}, limit={}", storeId, limit);
            List<Map<String, Object>> list = reviewMapper.selectByStoreId(storeId, limit);
            log.info("Found {} reviews", list.size());
            return list;
        } catch (Exception e) {
            log.error("Error getting reviews by store", e);
            throw new RuntimeException("查询评价失败: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "查看我的评价")
    @GetMapping("/my")
    public List<ReviewEntity> getMyReviews(@AuthenticationPrincipal AuthUser user) {
        try {
            log.info("Getting reviews for userId={}", user == null ? null : user.id());
            if (user == null) {
                throw new RuntimeException("用户未登录");
            }
            QueryWrapper<ReviewEntity> qw = new QueryWrapper<>();
            qw.eq("user_id", user.id());
            qw.orderByDesc("created_at");
            return reviewMapper.selectList(qw);
        } catch (Exception e) {
            log.error("Error getting my reviews", e);
            throw new RuntimeException("查询我的评价失败: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "删除我的评价")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user,
                       @PathVariable Long id) {
        try {
            log.info("Deleting review id={} for userId={}", id, user == null ? null : user.id());
            if (user == null) {
                throw new RuntimeException("用户未登录");
            }
            int rows = reviewMapper.deleteByIdAndUserId(id, user.id());
            if (rows == 0) {
                throw new RuntimeException("评价不存在或无权删除");
            }
            log.info("Review deleted, rows={}", rows);
        } catch (Exception e) {
            log.error("Error deleting review", e);
            throw new RuntimeException("删除评价失败: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> toMap(ReviewEntity r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("user_id", r.getUserId());
        m.put("store_id", r.getStoreId());
        m.put("order_id", r.getOrderId());
        m.put("rating", r.getRating());
        m.put("food_rating", r.getFoodRating());
        m.put("service_rating", r.getServiceRating());
        m.put("environment_rating", r.getEnvironmentRating());
        m.put("cat_rating", r.getCatRating());
        m.put("content", r.getContent());
        m.put("is_anonymous", r.getIsAnonymous());
        m.put("reply", r.getReply());
        m.put("replied_at", r.getRepliedAt());
        m.put("created_at", r.getCreatedAt());
        return m;
    }

    public record ReviewRequest(
        Long storeId,
        Long orderId,
        Integer rating,
        String content,
        Integer foodRating,
        Integer serviceRating,
        Integer environmentRating,
        Integer catRating,
        Boolean isAnonymous
    ) {}
}
