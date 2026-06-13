package com.nekocafe.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nekocafe.common.ApiException;
import com.nekocafe.domain.ReservationValidator;
import com.nekocafe.entity.PointTransaction;
import com.nekocafe.entity.User;
import com.nekocafe.mapper.PointTransactionMapper;
import com.nekocafe.mapper.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务：会员等级、积分管理（含流水）、资料维护
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PointTransactionMapper pointTransactionMapper;

    public UserService(UserMapper userMapper, PointTransactionMapper pointTransactionMapper) {
        this.userMapper = userMapper;
        this.pointTransactionMapper = pointTransactionMapper;
    }

    /**
     * 列出用户（支持角色筛选）
     */
    public List<Map<String, Object>> listUsers(String role) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        if (role != null && !role.isBlank()) {
            String[] roles = role.split(",");
            if (roles.length == 1) {
                qw.eq("role", roles[0].trim());
            } else {
                qw.in("role", Arrays.stream(roles).map(String::trim).collect(Collectors.toList()));
            }
        }
        qw.orderByAsc("id");
        List<User> users = userMapper.selectList(qw);
        return users.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("mobile_number", u.getMobileNumber());
            m.put("role", u.getRole());
            m.put("member_level", u.getMemberLevel());
            m.put("points", u.getPoints());
            m.put("preferences", u.getPreferences());
            m.put("created_at", u.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
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
     * 更新用户资料：仅允许本人维护姓名/手机号/偏好；
     * role、points、member_level 由系统规则维护，不接受该入口修改。
     */
    public Map<String, Object> updateProfile(Long userId, String name, String mobileNumber, List<String> preferences) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }

        if (name != null && !name.isBlank()) {
            user.setName(name.trim());
        }

        if (mobileNumber != null && !mobileNumber.isBlank()) {
            String mobile = ReservationValidator.normalizeMobile(mobileNumber);
            if (mobile.length() < 8) {
                throw ApiException.badRequest("手机号至少包含 8 位数字");
            }
            if (!mobile.equals(user.getMobileNumber())) {
                Long conflicts = userMapper.selectCount(
                        new QueryWrapper<User>().eq("mobile_number", mobile).ne("id", userId));
                if (conflicts != null && conflicts > 0) {
                    throw ApiException.conflict("该手机号已被其他账号使用");
                }
                user.setMobileNumber(mobile);
            }
        }

        if (preferences != null) {
            user.setPreferences(preferences);
        }

        try {
            userMapper.updateById(user);
        } catch (DuplicateKeyException ex) {
            // 并发改号时由 users.mobile_number 唯一约束兜底
            throw ApiException.conflict("该手机号已被其他账号使用");
        }

        return getProfile(userId);
    }

    /**
     * 积分变更统一入口：同一事务内更新 users.points/member_level 并写入积分流水。
     * (sourceType, sourceId) 命中唯一约束时抛 409，保证同一来源不会重复发放；
     * 任一步失败整体回滚，调用方事务（如预约完成流转）随之回滚。
     */
    @Transactional
    public Map<String, Object> changePoints(Long userId, int delta, String sourceType, Long sourceId, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }

        int newPoints = user.getPoints() + delta;
        if (newPoints < 0) {
            throw ApiException.badRequest("积分不足");
        }

        user.setPoints(newPoints);
        user.setMemberLevel(calculateMemberLevel(newPoints));
        userMapper.updateById(user);

        try {
            pointTransactionMapper.insert(
                    new PointTransaction(userId, delta, newPoints, sourceType, sourceId, reason));
        } catch (DuplicateKeyException ex) {
            throw ApiException.conflict("该来源的积分已发放，不可重复入账");
        }

        return getProfile(userId);
    }

    /**
     * 当前用户积分明细（最新在前），用于积分追溯展示。
     */
    public List<Map<String, Object>> getPointsHistory(Long userId) {
        return pointTransactionMapper.listByUser(userId);
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
