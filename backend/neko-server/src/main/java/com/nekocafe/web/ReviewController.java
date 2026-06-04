package com.nekocafe.web;

import com.nekocafe.common.ApiResponse;
import com.nekocafe.dto.ReviewRequest;
import com.nekocafe.security.AuthUser;
import com.nekocafe.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Reviews", description = "顾客评价")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "门店评价列表")
    @GetMapping("/api/reviews")
    public ApiResponse list(@RequestParam(required = false) Long storeId) {
        return ApiResponse.of(reviewService.listByStore(storeId));
    }

    @Operation(summary = "提交评价")
    @PostMapping("/api/reviews")
    public ApiResponse create(@RequestBody ReviewRequest req, @AuthenticationPrincipal AuthUser user) {
        return ApiResponse.of(reviewService.create(req, user.id()));
    }
}
