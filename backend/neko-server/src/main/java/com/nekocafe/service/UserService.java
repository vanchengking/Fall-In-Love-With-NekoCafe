package com.nekocafe.service;

import com.nekocafe.entity.User;
import com.nekocafe.mapper.UserMapper;
import com.nekocafe.common.ApiException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务：会员等级、积分管理、资料维护
 */
@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 获取用户资料
     */
    public Map<String, Object> getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("name", user.getName());
        profile.put("mobile_number", user.getMobileNumber());
        profile.put("role", user.getRole());
        profile.put("member_level", user.getMemberLevel());
        profile.put("points", user.getPoints());
        profile.put("preferences", user.getPreferences());
        return profile;
    }

    /**
     * 更新用户资料
     */
    public Map<String, Object> updateProfile(Long userId, String name, List<String> preferences) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }

        if (name != null && !name.isBlank()) {
            user.setName(name.trim());
        }

        if (preferences != null) {
            user.setPreferences(preferences);
        }

        userMapper.updateById(user);

        return getProfile(userId);
    }

    /**
     * 增加/扣减积分，自动更新会员等级
     */
    public Map<String, Object> addPoints(Long userId, Integer delta) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }

        int oldPoints = user.getPoints();
        int newPoints = oldPoints + delta;
        if (newPoints < 0) {
            throw ApiException.badRequest("积分不足");
        }

        System.out.println("[UserService] addPoints: userId=" + userId + ", delta=" + delta + ", oldPoints=" + oldPoints + ", newPoints=" + newPoints);

        user.setPoints(newPoints);
        user.setMemberLevel(calculateMemberLevel(newPoints));
        userMapper.updateById(user);

        return getProfile(userId);
    }

    /**
     * 获取会员信息（等级、积分、折扣）
     */
    public Map<String, Object> getMemberInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }

        String level = user.getMemberLevel();
        int points = user.getPoints();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("member_level", level);
        info.put("points", points);
        info.put("discount", getDiscount(level));
        info.put("next_level", getNextLevel(level));
        info.put("points_to_next_level", getPointsToNextLevel(level, points));
        return info;
    }

    /**
     * 根据积分计算会员等级
     * 青铜：0-999
     * 白银：1000-2999
     * 黄金：3000-4999
     * 铂金：5000+
     */
    private String calculateMemberLevel(int points) {
        if (points >= 5000) {
            return "platinum";
        } else if (points >= 3000) {
            return "gold";
        } else if (points >= 1000) {
            return "silver";
        } else {
            return "bronze";
        }
    }

    /**
     * 根据会员等级获取折扣
     * 青铜：无折扣 (1.0)
     * 白银：95折 (0.95)
     * 黄金：9折 (0.9)
     * 铂金：85折 (0.85)
     */
    private double getDiscount(String level) {
        return switch (level) {
            case "platinum" -> 0.85;
            case "gold" -> 0.9;
            case "silver" -> 0.95;
            default -> 1.0;
        };
    }

    /**
     * 根据用户ID获取折扣率（供订单服务调用）
     */
    public double getDiscountByUserId(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }
        return getDiscount(user.getMemberLevel());
    }

    /**
     * 获取下一等级
     */
    private String getNextLevel(String currentLevel) {
        return switch (currentLevel) {
            case "bronze" -> "silver";
            case "silver" -> "gold";
            case "gold" -> "platinum";
            case "platinum" -> null;
            default -> "silver";
        };
    }

    /**
     * 获取距离下一等级所需积分
     */
    private int getPointsToNextLevel(String currentLevel, int currentPoints) {
        return switch (currentLevel) {
            case "bronze" -> 1000 - currentPoints;
            case "silver" -> 3000 - currentPoints;
            case "gold" -> 5000 - currentPoints;
            case "platinum" -> 0;
            default -> 1000 - currentPoints;
        };
    }
}
