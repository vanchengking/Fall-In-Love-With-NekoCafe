package com.nekocafe.web;

import com.nekocafe.entity.ReviewEntity;
import com.nekocafe.mapper.ReviewMapper;
import com.nekocafe.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "评价管理")
@RequestMapping("/api/reviews")
@RestController
public class ReviewController {

    private final ReviewMapper reviewMapper;

    public ReviewController(ReviewMapper reviewMapper) {
        this.reviewMapper = reviewMapper;
    }

    @Operation(summary = "提交评价")
    @PostMapping
    public ReviewEntity create(@AuthenticationPrincipal AuthUser user,
                                @RequestBody ReviewRequest req) {
        ReviewEntity review = new ReviewEntity();
        review.setUserId(user.id());
        review.setStoreId(req.storeId());
        review.setReservationId(req.reservationId());
        review.setCatId(req.catId());
        review.setRating(req.rating());
        review.setContent(req.content());
        review.setPhotoUrls(req.photoUrls());
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        reviewMapper.insert(review);
        return review;
    }

    @Operation(summary = "查看门店评价")
    @GetMapping("/store/{storeId}")
    public List<ReviewEntity> getByStore(@PathVariable Long storeId,
                                          @RequestParam(defaultValue = "20") Integer limit) {
        return reviewMapper.selectByStoreId(storeId, limit);
    }

    @Operation(summary = "查看我的评价")
    @GetMapping("/my")
    public List<ReviewEntity> getMyReviews(@AuthenticationPrincipal AuthUser user) {
        return reviewMapper.selectByUserId(user.id());
    }

    @Operation(summary = "删除我的评价")
    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal AuthUser user,
                      @PathVariable Long id) {
        reviewMapper.deleteByIdAndUserId(id, user.id());
    }

    public record ReviewRequest(
        Long storeId,
        Long reservationId,
        Long catId,
        Integer rating,
        String content,
        String photoUrls
    ) {}
}
