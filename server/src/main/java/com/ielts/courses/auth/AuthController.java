package com.ielts.courses.auth;

import com.ielts.courses.common.ApiResult;
import com.ielts.courses.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final SmsCodeService smsCodeService;

    public AuthController(AuthService authService, SmsCodeService smsCodeService) {
        this.authService = authService;
        this.smsCodeService = smsCodeService;
    }

    @PostMapping("/phone/send-code")
    public ApiResult<Map<String, String>> sendCode(@Valid @RequestBody SendCodeReq req) {
        String code = smsCodeService.sendCode(req.phone);
        log.info("[SMS] 验证码已发送 phone={}, code={}", req.phone, code);
        // 开发环境把验证码返回给前端，方便联调；生产环境建议移除该字段
        return ApiResult.ok(Map.of("message", "验证码已发送", "mockCode", code));
    }

    @PostMapping("/phone/login")
    public ApiResult<AuthService.LoginResult> login(@Valid @RequestBody LoginReq req) {
        return ApiResult.ok(authService.loginOrRegister(req.phone, req.code));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@CurrentUser LoginUser user) {
        if (user != null) {
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

    public record SendCodeReq(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
            String phone
    ) {
    }

    public record LoginReq(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
            String phone,
            @NotBlank(message = "验证码不能为空")
            @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
            String code
    ) {
    }
}
