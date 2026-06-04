package com.nekocafe.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nekocafe.common.ApiResponse;
import com.nekocafe.common.Payloads;
import com.nekocafe.dto.AuthRequests;
import com.nekocafe.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "短信验证码沙箱登录")
public class AuthController {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public AuthController(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "发送短信验证码（沙箱）", description = "沙箱模式直接返回验证码并写入 Redis")
    @PostMapping("/sms/send")
    public ApiResponse sendCode(@RequestBody Map<String, Object> body) {
        AuthRequests.SmsRequest req = objectMapper.convertValue(Payloads.unwrap(body), AuthRequests.SmsRequest.class);
        return ApiResponse.of(authService.sendCode(req));
    }

    @Operation(summary = "登录", description = "校验验证码后 upsert 用户并签发 JWT")
    @PostMapping("/login")
    public ApiResponse login(@RequestBody Map<String, Object> body) {
        AuthRequests.LoginRequest req = objectMapper.convertValue(Payloads.unwrap(body), AuthRequests.LoginRequest.class);
        return ApiResponse.of(authService.login(req));
    }
}
