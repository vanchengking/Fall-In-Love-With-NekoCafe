package com.nekocafe.web;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nekocafe.common.ApiResponse;
import com.nekocafe.entity.AuditLog;
import com.nekocafe.entity.User;
import com.nekocafe.mapper.AuditLogMapper;
import com.nekocafe.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "审计日志", description = "操作审计日志查询")
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;
    private final UserMapper userMapper;

    public AuditLogController(AuditLogMapper auditLogMapper, UserMapper userMapper) {
        this.auditLogMapper = auditLogMapper;
        this.userMapper = userMapper;
    }

    @Operation(summary = "查询审计日志")
    @GetMapping
    public ApiResponse list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetTable,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {

        QueryWrapper<AuditLog> qw = new QueryWrapper<>();
        if (action != null && !action.isBlank()) {
            qw.eq("action", action);
        }
        if (targetTable != null && !targetTable.isBlank()) {
            qw.eq("target_type", targetTable);
        }
        if (startDate != null && !startDate.isBlank()) {
            qw.ge("created_at", startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            qw.le("created_at", endDate);
        }
        qw.orderByDesc("created_at");
        qw.last("LIMIT " + Math.min(limit, 500));

        List<AuditLog> logs = auditLogMapper.selectList(qw);

        List<Map<String, Object>> result = logs.stream().map(log -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", log.getId());
            m.put("created_at", log.getCreatedAt());
            m.put("user_name", getUserName(log.getActorUserId()));
            m.put("action", log.getAction());
            m.put("target_table", log.getTargetType());
            m.put("target_id", log.getTargetId());
            m.put("detail", parseDetail(log.getDetail()));
            m.put("ip_address", "");
            return m;
        }).collect(Collectors.toList());

        return ApiResponse.of(result);
    }

    private String getUserName(Long userId) {
        if (userId == null) return "系统";
        User user = userMapper.selectById(userId);
        return user != null ? user.getName() : "用户#" + userId;
    }

    private Map<String, Object> parseDetail(String detail) {
        if (detail == null || detail.isBlank()) return null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            return om.readValue(detail, Map.class);
        } catch (Exception e) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("raw", detail);
            return m;
        }
    }
}
