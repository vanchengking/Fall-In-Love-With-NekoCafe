package com.nekocafe.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nekocafe.common.ApiException;
import com.nekocafe.domain.ReservationValidator;
import com.nekocafe.dto.AuthRequests;
import com.nekocafe.entity.User;
import com.nekocafe.mapper.UserMapper;
import com.nekocafe.security.JwtService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 短信验证码沙箱登录：发送验证码 -> 校验 -> upsert 用户 -> 签发 JWT。
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final SmsService smsService;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper, SmsService smsService, JwtService jwtService) {
        this.userMapper = userMapper;
        this.smsService = smsService;
        this.jwtService = jwtService;
    }

    public Map<String, Object> sendCode(AuthRequests.SmsRequest req) {
        String mobile = ReservationValidator.normalizeMobile(req == null ? null : req.mobileNumber());
        if (mobile.length() < 8) {
            throw ApiException.badRequest("mobileNumber must contain at least 8 digits");
        }
        String code = smsService.issueCode(mobile);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mobileNumber", mobile);
        out.put("sandbox", true);
        // 沙箱模式下直接返回验证码，方便演示；生产应改为真实下发
        out.put("code", code);
        out.put("expiresInSeconds", smsService.ttlSeconds());
        return out;
    }

    public Map<String, Object> login(AuthRequests.LoginRequest req) {
        if (req == null) {
            throw ApiException.badRequest("request body is required");
        }
        String mobile = ReservationValidator.normalizeMobile(req.mobileNumber());
        if (mobile.length() < 8) {
            throw ApiException.badRequest("mobileNumber must contain at least 8 digits");
        }
        if (!smsService.verify(mobile, req.code())) {
            throw ApiException.unauthorized("invalid verification code");
        }

        User user = upsert(mobile, req);
        String token = jwtService.issue(user.getId(), user.getRole(), user.getName(), user.getMobileNumber());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("user", user);
        out.put("token", token);
        return out;
    }

    private User upsert(String mobile, AuthRequests.LoginRequest req) {
        User existing = userMapper.selectOne(new QueryWrapper<User>().eq("mobile_number", mobile));
        List<String> prefs = req.preferences() == null ? new ArrayList<>() : new ArrayList<>(req.preferences());

        if (existing == null) {
            User created = new User();
            created.setName(req.name() == null || req.name().isBlank() ? "NekoCafe 用户" : req.name().trim());
            created.setMobileNumber(mobile);
            created.setRole(req.role() == null || req.role().isBlank() ? "customer" : req.role().trim());
            created.setMemberLevel("silver");
            created.setPoints(0);
            created.setPreferences(prefs);
            userMapper.insert(created);
            return created;
        }

        // 已有用户保留其既有角色（演示种子账号角色固定），仅在提供时更新姓名/偏好
        if (req.name() != null && !req.name().isBlank()) {
            existing.setName(req.name().trim());
        }
        if (!prefs.isEmpty()) {
            existing.setPreferences(prefs);
        }
        userMapper.updateById(existing);
        return existing;
    }
}
