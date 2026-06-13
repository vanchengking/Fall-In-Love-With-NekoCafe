package com.nekocafe.web;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nekocafe.common.ApiResponse;
import com.nekocafe.entity.Coupon;
import com.nekocafe.mapper.CouponMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/coupons")
@Tag(name = "优惠券管理")
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

        List<Map<String, Object>> result = coupons.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("code", c.getCode());
            m.put("title", c.getTitle());
            m.put("discount_cents", c.getDiscountCents());
            m.put("min_spend_cents", c.getMinSpendCents());
            m.put("valid_from", c.getValidFrom());
            m.put("valid_to", c.getValidTo());
            m.put("status", c.getStatus());
            m.put("claimed_count", 0);
            m.put("used_count", 0);
            return m;
        }).collect(Collectors.toList());

        return ApiResponse.of(result);
    }

    @Operation(summary = "创建优惠券")
    @PostMapping
    public ApiResponse create(@RequestBody Map<String, Object> body) {
        Map<String, Object> data = (Map<String, Object>) body.getOrDefault("data", body);
        Coupon coupon = new Coupon();
        coupon.setCode((String) data.get("code"));
        coupon.setTitle((String) data.get("title"));
        coupon.setDiscountCents(toInt(data.get("discount_cents")));
        coupon.setMinSpendCents(toInt(data.get("min_spend_cents")));
        coupon.setValidFrom((String) data.get("valid_from"));
        coupon.setValidTo((String) data.get("valid_to"));
        coupon.setStatus("active");
        couponMapper.insert(coupon);
        return ApiResponse.of(Map.of("id", coupon.getId(), "success", true));
    }

    @Operation(summary = "更新优惠券")
    @PatchMapping("/{id}")
    public ApiResponse update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> data = (Map<String, Object>) body.getOrDefault("data", body);
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) return ApiResponse.of(Map.of("error", "not found"));
        if (data.containsKey("code")) coupon.setCode((String) data.get("code"));
        if (data.containsKey("title")) coupon.setTitle((String) data.get("title"));
        if (data.containsKey("discount_cents")) coupon.setDiscountCents(toInt(data.get("discount_cents")));
        if (data.containsKey("min_spend_cents")) coupon.setMinSpendCents(toInt(data.get("min_spend_cents")));
        if (data.containsKey("valid_from")) coupon.setValidFrom((String) data.get("valid_from"));
        if (data.containsKey("valid_to")) coupon.setValidTo((String) data.get("valid_to"));
        if (data.containsKey("status")) coupon.setStatus((String) data.get("status"));
        couponMapper.updateById(coupon);
        return ApiResponse.of(Map.of("success", true));
    }

    @Operation(summary = "删除优惠券")
    @DeleteMapping("/{id}")
    public ApiResponse delete(@PathVariable Long id) {
        couponMapper.deleteById(id);
        return ApiResponse.of(Map.of("success", true));
    }

    private Integer toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        return Integer.parseInt(v.toString());
    }
}
