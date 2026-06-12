package com.nekocafe.service;

import com.nekocafe.common.ApiException;
import com.nekocafe.dto.AuthRequests;
import com.nekocafe.entity.User;
import com.nekocafe.mapper.UserMapper;
import com.nekocafe.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 登录单元测试（FR-AUTH-001 / FR-AUTH-002）：
 * 验证码 123456 可登录、错误验证码 401、响应含 access_token、JWT 解码可见 userId 与 role。
 */
class AuthServiceTest {

    private static final String SECRET = "test-secret-0123456789abcdef-0123456789abcdef";

    private UserMapper userMapper;
    private SmsService smsService;
    private JwtService jwtService;
    private AuthService service;

    @BeforeEach
    void setup() {
        userMapper = mock(UserMapper.class);
        smsService = mock(SmsService.class);
        jwtService = new JwtService(SECRET, 3600);
        service = new AuthService(userMapper, smsService, jwtService);
    }

    private User user(long id, String role) {
        User u = new User();
        u.setId(id);
        u.setName("林小满");
        u.setMobileNumber("13800000001");
        u.setRole(role);
        u.setMemberLevel("gold");
        u.setPoints(1280);
        return u;
    }

    @Test
    @DisplayName("验证码 123456 正确：登录成功，响应含 access_token/token_type/expires_in/user")
    void loginSuccessReturnsAccessToken() {
        when(smsService.verify("13800000001", "123456")).thenReturn(true);
        when(userMapper.selectOne(any())).thenReturn(user(42L, "customer"));

        Map<String, Object> out = service.login(
                new AuthRequests.LoginRequest(null, "13800000001", "123456", null, null));

        assertNotNull(out.get("access_token"));
        assertEquals("Bearer", out.get("token_type"));
        assertEquals(3600L, out.get("expires_in"));
        assertNotNull(out.get("user"));
        // 兼容字段 token 与 access_token 一致
        assertEquals(out.get("access_token"), out.get("token"));
    }

    @Test
    @DisplayName("JWT 解码可见显式 userId 与 role claim（FR-AUTH-002）")
    void jwtCarriesUserIdAndRole() {
        when(smsService.verify("13800000001", "123456")).thenReturn(true);
        when(userMapper.selectOne(any())).thenReturn(user(42L, "customer"));

        Map<String, Object> out = service.login(
                new AuthRequests.LoginRequest(null, "13800000001", "123456", null, null));

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims((String) out.get("access_token"))
                .getPayload();
        assertEquals(42L, ((Number) claims.get("userId")).longValue());
        assertEquals("customer", claims.get("role"));
        assertEquals("42", claims.getSubject());
    }

    @Test
    @DisplayName("验证码错误：抛 401")
    void wrongCodeRejected() {
        when(smsService.verify("13800000001", "000000")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.login(new AuthRequests.LoginRequest(null, "13800000001", "000000", null, null)));
        assertEquals(401, ex.getStatus());
    }

    @Test
    @DisplayName("手机号不足 8 位数字：抛 400")
    void invalidMobileRejected() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.login(new AuthRequests.LoginRequest(null, "123", "123456", null, null)));
        assertEquals(400, ex.getStatus());
    }

    @Test
    @DisplayName("首次登录自动注册为 customer 角色")
    void firstLoginCreatesCustomer() {
        when(smsService.verify("13900000001", "123456")).thenReturn(true);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(100L);
            return 1;
        });

        Map<String, Object> out = service.login(
                new AuthRequests.LoginRequest(null, "13900000001", "123456", null, null));

        User created = (User) out.get("user");
        assertEquals("customer", created.getRole());
        assertNotNull(out.get("access_token"));
    }
}
