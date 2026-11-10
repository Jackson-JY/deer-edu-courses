package com.ielts.courses.auth;

import com.ielts.courses.audit.EventLogService;
import com.ielts.courses.common.ApiException;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EventLogService eventLogService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, EventLogService eventLogService,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.eventLogService = eventLogService;
        this.passwordResetService = passwordResetService;
    }

    /** 用户注册：手机号 + 密码 + 昵称（无需验证码） */
    @PostMapping("/register")
    public ApiResult<AuthService.LoginResult> register(@Valid @RequestBody RegisterReq req) {
        AuthService.LoginResult result = authService.register(req.phone, req.password, req.nickname);
        eventLogService.recordEvent(result.profile().id(), "REGISTER", null, null,
                result.profile().nickname(), "手机号注册");
        eventLogService.recordEvent(result.profile().id(), "LOGIN", null, null,
                result.profile().nickname(), "注册后自动登录");
        return ApiResult.ok(result);
    }

    /** 密码登录 */
    @PostMapping("/login")
    public ApiResult<AuthService.LoginResult> login(@Valid @RequestBody LoginReq req) {
        AuthService.LoginResult result;
        try {
            result = authService.loginByPassword(req.phone, req.password);
        } catch (ApiException e) {
            String maskedPhone = maskPhone(req.phone);
            eventLogService.recordEvent(null, "LOGIN_FAIL", null, null, null,
                    maskedPhone + "：" + e.getMessage());
            // 管理员账号登录失败同时进管理员操作日志
            if (authService.isAdminPhone(req.phone)) {
                eventLogService.recordAdminLogin(null, "手机号 " + maskedPhone, false, e.getMessage());
            }
            throw e;
        }
        Long uid = result.profile().id();
        boolean isAdmin = result.profile().roles().contains("ADMIN");
        eventLogService.recordEvent(uid, "LOGIN", null, null, result.profile().nickname(),
                isAdmin ? "管理员登录" : "密码登录");
        if (isAdmin) {
            eventLogService.recordAdminLogin(uid, result.profile().nickname(), true, null);
        }
        return ApiResult.ok(result);
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@CurrentUser LoginUser user) {
        if (user != null) {
            eventLogService.recordEvent(user.userId(), "LOGOUT", null, null,
                    user.isAdmin() ? "管理员退出" : "退出登录", null);
            authService.logout(user.userId());
        }
        return ApiResult.ok();
    }

    @GetMapping("/me/profile")
    public ApiResult<AuthService.UserProfile> profile(@CurrentUser LoginUser user) {
        if (user == null) {
            return ApiResult.error(401, "未登录", "NEED_LOGIN");
        }
        return ApiResult.ok(authService.getProfile(user.userId()));
    }

    /** 已登录用户修改密码（校验原密码，保持当前会话） */
    @PostMapping("/change-password")
    public ApiResult<Void> changePassword(@CurrentUser LoginUser user,
                                          @Valid @RequestBody ChangePasswordReq req) {
        if (user == null) {
            return ApiResult.error(401, "未登录", "NEED_LOGIN");
        }
        passwordResetService.changePassword(user.userId(), req.oldPassword(), req.newPassword());
        return ApiResult.ok();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone == null ? "" : phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    public record RegisterReq(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
            String phone,
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
            String password,
            String nickname
    ) {
    }

    public record LoginReq(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
            String phone,
            @NotBlank(message = "密码不能为空")
            String password
    ) {
    }

    public record ChangePasswordReq(
            @NotBlank(message = "原密码不能为空")
            String oldPassword,
            @NotBlank(message = "新密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
            String newPassword
    ) {
    }
}
