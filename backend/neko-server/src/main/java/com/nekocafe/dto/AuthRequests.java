package com.nekocafe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 鉴权相关请求体。
 */
public final class AuthRequests {

    private AuthRequests() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SmsRequest(String mobileNumber) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoginRequest(
            String name,
            String mobileNumber,
            String code,
            String role,
            List<String> preferences) {
    }
}
