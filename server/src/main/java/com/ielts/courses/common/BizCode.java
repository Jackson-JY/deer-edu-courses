package com.ielts.courses.common;

/**
 * 业务错误码，前端据此做精准跳转（被踢→重登、未购→购买、未绑→绑定手机）。
 * 与 docs/openapi.yaml#ApiError.bizCode 对齐。
 */
public enum BizCode {
    SESSION_KICKED(40101, "账号已在其他设备登录"),
    NEED_BIND_PHONE(40102, "请先绑定手机号"),
    NO_ENTITLEMENT(40301, "无观看权限"),
    COURSE_EXPIRED(40302, "课程已过期"),
    COUPON_USED(40901, "优惠券已使用"),
    BAD_REQUEST(40000, "参数错误"),
    NOT_FOUND(40400, "资源不存在");

    private final int code;
    private final String message;

    BizCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
