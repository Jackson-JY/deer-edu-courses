package com.ielts.courses.auth;

import com.ielts.courses.common.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 找回密码（匿名可访问，/api/auth/** 已在 SecurityConfig 放行） */
@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /** 提交找回密码申请；无论手机号是否注册都返回相同提示 */
    @PostMapping("/request")
    public ApiResult<Void> request(@Valid @RequestBody RequestReq req, HttpServletRequest http) {
        passwordResetService.requestReset(req.phone(), clientIp(http));
        return ApiResult.ok();
    }

    /** 凭手机号 + 重置码设置新密码 */
    @PostMapping("/confirm")
    public ApiResult<Void> confirm(@Valid @RequestBody ConfirmReq req) {
        passwordResetService.confirmReset(req.phone(), req.code(), req.newPassword());
        return ApiResult.ok();
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            String ip = comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
            return ip.length() > 64 ? ip.substring(0, 64) : ip;
        }
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real.trim().length() > 64 ? real.trim().substring(0, 64) : real.trim();
        }
        return req.getRemoteAddr();
    }

    public record RequestReq(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
            String phone
    ) {
    }

    public record ConfirmReq(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
            String phone,
            @NotBlank(message = "重置码不能为空")
            @Pattern(regexp = "^\\d{6}$", message = "重置码为 6 位数字")
            String code,
            @NotBlank(message = "新密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
            String newPassword
    ) {
    }
}
