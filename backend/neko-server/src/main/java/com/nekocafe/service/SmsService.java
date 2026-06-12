package com.nekocafe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 短信验证码沙箱：验证码写入 Redis（带 TTL），登录时优先校验 Redis；
 * 同时保留固定演示码作为兜底，确保评委演示稳定。Redis 不可用时降级为固定码。
 */
@Service
public class SmsService {

    private static final String KEY_PREFIX = "neko:sms:";

    private final StringRedisTemplate redis;
    private final String fixedCode;
    private final long ttlSeconds;

    public SmsService(StringRedisTemplate redis,
                      @Value("${neko.sms.fixed-code:123456}") String fixedCode,
                      @Value("${neko.sms.ttl-seconds:300}") long ttlSeconds) {
        this.redis = redis;
        this.fixedCode = fixedCode;
        this.ttlSeconds = ttlSeconds;
    }

    public String issueCode(String mobile) {
        String code = fixedCode;
        try {
            redis.opsForValue().set(KEY_PREFIX + mobile, code, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // Redis 暂不可用时仍返回固定演示码
        }
        return code;
    }

    public boolean verify(String mobile, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        try {
            String stored = redis.opsForValue().get(KEY_PREFIX + mobile);
            if (stored != null && stored.equals(code)) {
                redis.delete(KEY_PREFIX + mobile);
                return true;
            }
        } catch (Exception ignored) {
            // 忽略 Redis 异常，走固定码兜底
        }
        return fixedCode.equals(code);
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }
}
