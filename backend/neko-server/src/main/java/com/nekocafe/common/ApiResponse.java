package com.nekocafe.common;

/**
 * 统一成功响应包装：序列化为 {"data": ...}，与前端既有契约一致。
 */
public record ApiResponse(Object data) {
    public static ApiResponse of(Object data) {
        return new ApiResponse(data);
    }
}
