package com.nekocafe.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nekocafe.common.ApiException;
import com.nekocafe.dto.ReviewRequest;
import com.nekocafe.entity.Review;
import com.nekocafe.mapper.ReviewMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewMapper reviewMapper;

    public ReviewService(ReviewMapper reviewMapper) {
        this.reviewMapper = reviewMapper;
    }

    public List<Map<String, Object>> listByStore(Long storeId) {
        QueryWrapper<Review> qw = new QueryWrapper<>();
        if (storeId != null) {
            qw.eq("store_id", storeId);
        }
        qw.orderByDesc("created_at");
        return reviewMapper.selectList(qw).stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String, Object> create(ReviewRequest req, Long userId) {
        if (req.storeId() == null) throw ApiException.badRequest("storeId is required");
        if (req.rating() == null || req.rating() < 1 || req.rating() > 5) throw ApiException.badRequest("rating must be between 1 and 5");

        Review review = new Review();
        review.setUserId(userId);
        review.setStoreId(req.storeId());
        review.setReservationId(req.reservationId());
        review.setCatId(req.catId());
        review.setRating(req.rating());
        review.setContent(req.content());
        reviewMapper.insert(review);
        return toMap(review);
    }

    private Map<String, Object> toMap(Review r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("reservation_id", r.getReservationId());
        m.put("user_id", r.getUserId());
        m.put("store_id", r.getStoreId());
        m.put("cat_id", r.getCatId());
        m.put("rating", r.getRating());
        m.put("content", r.getContent());
        return m;
    }
}
