package com.nekocafe.web;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nekocafe.common.ApiResponse;
import com.nekocafe.entity.User;
import com.nekocafe.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "用户管理")
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Operation(summary = "用户列表")
    @GetMapping
    public ApiResponse list(@RequestParam(required = false) String role) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        if (role != null && !role.isBlank()) {
            qw.in("role", role.split(","));
        }
        qw.select("id", "name", "mobile_number", "role", "member_level", "points", "preferences", "created_at");
        qw.orderByAsc("id");
        List<User> users = userMapper.selectList(qw);
        return ApiResponse.of(users);
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public ApiResponse detail(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        return ApiResponse.of(user);
    }

    @Operation(summary = "更新用户信息")
    @PatchMapping("/{id}")
    public ApiResponse update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return ApiResponse.of(Map.of("error", "用户不存在"));
        }
        if (body.containsKey("name")) {
            user.setName((String) body.get("name"));
        }
        if (body.containsKey("role")) {
            user.setRole((String) body.get("role"));
        }
        if (body.containsKey("memberLevel")) {
            user.setMemberLevel((String) body.get("memberLevel"));
        }
        userMapper.updateById(user);
        return ApiResponse.of(user);
    }
}
