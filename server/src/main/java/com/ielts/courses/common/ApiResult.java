package com.ielts.courses.common;

/**
 * 统一响应包裹，对应 docs/openapi.yaml#ApiResult。
 * code=0 表示成功；非 0 为业务错误，配合 bizCode 使用。
 */
public record ApiResult<T>(int code, String message, T data, String bizCode) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "ok", data, null);
    }

    public static <T> ApiResult<T> ok() {
        return new ApiResult<>(0, "ok", null, null);
    }

    public static <T> ApiResult<T> error(int code, String message, String bizCode) {
        return new ApiResult<>(code, message, null, bizCode);
    }
}
