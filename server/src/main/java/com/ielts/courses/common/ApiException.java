package com.ielts.courses.common;

/** 业务异常，由 GlobalExceptionHandler 统一转 ApiResult。 */
public class ApiException extends RuntimeException {

    private final BizCode bizCode;

    public ApiException(BizCode bizCode) {
        super(bizCode.message());
        this.bizCode = bizCode;
    }

    public ApiException(BizCode bizCode, String message) {
        super(message);
        this.bizCode = bizCode;
    }

    public BizCode bizCode() {
        return bizCode;
    }
}
