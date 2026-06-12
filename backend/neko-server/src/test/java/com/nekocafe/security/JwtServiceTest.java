package com.nekocafe.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * JWT 签发/解析单元测试。
 */
class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("test-secret-0123456789abcdef-0123456789abcdef", 3600);

    @Test
    @DisplayName("签发后可解析出 id/name/mobile/role")
    void issueAndParseRoundtrip() {
        String token = jwtService.issue(7L, "staff", "高店员", "13800000002");

        AuthUser parsed = jwtService.parse(token);

        assertEquals(7L, parsed.id());
        assertEquals("staff", parsed.role());
        assertEquals("高店员", parsed.name());
        assertEquals("13800000002", parsed.mobile());
    }

    @Test
    @DisplayName("非法/篡改 token 解析返回 null")
    void invalidTokenReturnsNull() {
        assertNull(jwtService.parse("not-a-jwt"));
        assertNull(jwtService.parse(jwtService.issue(7L, "staff", "x", "1") + "tampered"));
    }

    @Test
    @DisplayName("ttlSeconds 返回配置值（登录响应 expires_in 来源）")
    void ttlSecondsExposed() {
        assertEquals(3600L, jwtService.ttlSeconds());
    }
}
