package com.nekocafe.security;

/**
 * 已认证用户主体，存放于 Spring Security 上下文的 principal 中。
 */
public record AuthUser(Long id, String name, String mobile, String role) {
}
