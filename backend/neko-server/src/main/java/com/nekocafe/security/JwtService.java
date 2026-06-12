package com.nekocafe.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与校验（HS256 无状态 Token）。
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(
            @Value("${neko.jwt.secret}") String secret,
            @Value("${neko.jwt.ttl-seconds:86400}") long ttlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(Long userId, String role, String name, String mobile) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                // FR-AUTH-002：除 sub 外显式携带 userId 与 role，便于网关/前端直接解码使用
                .claim("userId", userId)
                .claim("role", role)
                .claim("name", name)
                .claim("mobile", mobile)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlSeconds * 1000))
                .signWith(key)
                .compact();
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }

    /** 解析并校验签名/有效期，失败返回 null。 */
    public AuthUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long id = Long.valueOf(claims.getSubject());
            return new AuthUser(
                    id,
                    claims.get("name", String.class),
                    claims.get("mobile", String.class),
                    claims.get("role", String.class));
        } catch (Exception ex) {
            return null;
        }
    }
}
