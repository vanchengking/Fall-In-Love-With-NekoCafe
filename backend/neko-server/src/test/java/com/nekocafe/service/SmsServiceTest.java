package com.nekocafe.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 短信验证码沙箱单元测试：固定码 123456 兜底（Redis 不可用）与 Redis 验证码优先。
 */
class SmsServiceTest {

    @Test
    @DisplayName("Redis 不可用时固定码 123456 兜底：正确码通过，错误码拒绝")
    void fixedCodeFallbackWhenRedisDown() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new RuntimeException("redis down"));
        SmsService sms = new SmsService(redis, "123456", 300);

        assertEquals("123456", sms.issueCode("13800000001"));
        assertTrue(sms.verify("13800000001", "123456"));
        assertFalse(sms.verify("13800000001", "8888"));
        assertFalse(sms.verify("13800000001", null));
        assertFalse(sms.verify("13800000001", " "));
    }

    @Test
    @DisplayName("Redis 中验证码命中：校验通过并删除，防止重放")
    @SuppressWarnings("unchecked")
    void redisStoredCodeMatchesAndIsConsumed() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("neko:sms:13800000001")).thenReturn("123456");
        SmsService sms = new SmsService(redis, "123456", 300);

        assertTrue(sms.verify("13800000001", "123456"));
        verify(redis).delete("neko:sms:13800000001");
    }
}
