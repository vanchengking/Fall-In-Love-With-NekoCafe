package com.nekocafe.web;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nekocafe.common.ApiResponse;
import com.nekocafe.entity.Coupon;
import com.nekocafe.mapper.CouponMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
@Tag(name = "Coupons", description = "优惠券管理")
public class CouponController {

    private final CouponMapper couponMapper;

    public CouponController(CouponMapper couponMapper) {
        this.couponMapper = couponMapper;
    }

    @Operation(summary = "优惠券列表")
    @GetMapping
    public ApiResponse list() {
        QueryWrapper<Coupon> qw = new QueryWrapper<>();
        qw.orderByDesc("created_at");
        List<Coupon> coupons = couponMapper.selectList(qw);
        return ApiResponse.of(coupons);
    }

    @Operation(summary = "创建优惠券")
    @PostMapping
    public ApiResponse create(@RequestBody Coupon coupon) {
        if (coupon.getStatus() == null) {
            coupon.setStatus("active");
        }
        couponMapper.insert(coupon);
        return ApiResponse.of(coupon);
    }

    @Operation(summary = "更新优惠券")
    @PatchMapping("/{id}")
    public ApiResponse update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            return ApiResponse.of(Map.of("error", "优惠券不存在"));
        }
        if (body.containsKey("status")) {
            coupon.setStatus((String) body.get("status"));
        }
        if (body.containsKey("title")) {
            coupon.setTitle((String) body.get("title"));
        }
        couponMapper.updateById(coupon);
        return ApiResponse.of(coupon);
    }
}
