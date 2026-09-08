package com.ielts.courses.auth;

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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 用户注册：手机号 + 密码 + 昵称（无需验证码） */
    @PostMapping("/register")
    public ApiResult<AuthService.LoginResult> register(@Valid @RequestBody RegisterReq req) {
        return ApiResult.ok(authService.register(req.phone, req.password, req.nickname));
    }

    /** 密码登录 */
    @PostMapping("/login")
    public ApiResult<AuthService.LoginResult> login(@Valid @RequestBody LoginReq req) {
        return ApiResult.ok(authService.loginByPassword(req.phone, req.password));
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
}
