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
 * GET  /api/users/me                 当前用户资料
 * PUT  /api/users/me                 更新姓名/手机号/偏好
 * GET  /api/users/me/member          会员等级与权益
 * GET  /api/users/me/points/history  积分明细（最新在前）
 *
 * 积分不提供自助修改接口：积分仅由业务规则发放（预约完成、订单支付/撤销），
 * 全部经 {@code UserService.changePoints} 落积分流水。
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

    @Operation(summary = "更新当前用户资料", description = "可更新姓名/手机号/偏好；角色、积分、会员等级不可经此修改")
    @PutMapping("/me")
    public ApiResponse updateProfile(@RequestBody Map<String, Object> body,
                                     @AuthenticationPrincipal AuthUser user) {
        // 前端 http.ts 会用 wrapData() 包装一层 {data: ...}，需要解包
        Map<String, Object> payload = (Map<String, Object>) Payloads.unwrap(body);
        Object nameRaw = payload.get("name");
        String name = nameRaw == null ? null : String.valueOf(nameRaw);
        Object mobileRaw = payload.get("mobileNumber");
        if (mobileRaw == null) {
            mobileRaw = payload.get("mobile_number");
        }
        String mobileNumber = mobileRaw == null ? null : String.valueOf(mobileRaw);
        List<String> prefs = payload.containsKey("preferences") ?
                (List<String>) payload.get("preferences") : null;
        return ApiResponse.of(userService.updateProfile(user.id(), name, mobileNumber, prefs));
    }

    @Operation(summary = "获取会员等级与权益")
    @GetMapping("/me/member")
    public ApiResponse memberInfo(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.of(userService.getMemberInfo(user.id()));
    }

    @Operation(summary = "获取积分明细", description = "按时间倒序返回 delta/balance_after/source_type/source_id/reason/created_at")
    @GetMapping("/me/points/history")
    public ApiResponse pointsHistory(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.of(userService.getPointsHistory(user.id()));
    }
}
