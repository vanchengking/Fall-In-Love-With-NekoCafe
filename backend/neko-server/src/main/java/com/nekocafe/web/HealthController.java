package com.nekocafe.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Health", description = "健康检查")
public class HealthController {

    @Operation(summary = "健康检查", description = "评委一键启动后用于探活：GET /healthz")
    @GetMapping("/healthz")
    public Map<String, Object> healthz() {
        return Map.of("ok", true, "service", "neko-server");
    }
}
