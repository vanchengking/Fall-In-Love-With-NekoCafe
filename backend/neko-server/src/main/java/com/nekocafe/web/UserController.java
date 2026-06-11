package com.nekocafe.web;

import com.nekocafe.common.ApiResponse;
import com.nekocafe.common.Payloads;
import com.nekocafe.security.AuthUser;
import com.nekocafe.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户资料与会员管理
 * GET  /api/users/me          当前用户资料
 * PUT  /api/users/me          更新姓名/偏好
 * POST /api/users/me/points   增加积分（内部调用）
 * GET  /api/users/me/member   会员等级与权益
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "用户资料与会员管理")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "获取当前用户资料")
    @GetMapping("/me")
    public ApiResponse getProfile(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.of(userService.getProfile(user.id()));
    }

    @Operation(summary = "更新当前用户资料", description = "可更新姓名/偏好")
    @PutMapping("/me")
    public ApiResponse updateProfile(@RequestBody Map<String, Object> body,
                                     @AuthenticationPrincipal AuthUser user) {
        // 前端 http.ts 会用 wrapData() 包装一层 {data: ...}，需要解包
        Map<String, Object> payload = (Map<String, Object>) Payloads.unwrap(body);
        String name = payload.containsKey("name") ? payload.get("name").toString() : null;
        List<String> prefs = payload.containsKey("preferences") ?
                (List<String>) payload.get("preferences") : null;
        return ApiResponse.of(userService.updateProfile(user.id(), name, prefs));
    }

    @Operation(summary = "增加积分", description = "delta 为正表示增加，为负表示扣减")
    @PostMapping("/me/points")
    public ApiResponse addPoints(@RequestBody Map<String, Object> body,
                                 @AuthenticationPrincipal AuthUser user) {
        if (!body.containsKey("delta")) {
            throw new RuntimeException("delta is required");
        }
        Integer delta = Integer.valueOf(body.get("delta").toString());
        return ApiResponse.of(userService.addPoints(user.id(), delta));
    }

    @Operation(summary = "获取会员等级与权益")
    @GetMapping("/me/member")
    public ApiResponse memberInfo(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.of(userService.getMemberInfo(user.id()));
    }
}
