package com.ielts.courses.admin;

import com.ielts.courses.auth.PasswordResetRequest;
import com.ielts.courses.auth.PasswordResetService;
import com.ielts.courses.auth.LoginUser;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.common.CurrentUser;
import com.ielts.courses.user.User;
import com.ielts.courses.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 管理端：密码重置申请处理 */
@RestController
@RequestMapping("/api/admin/password-resets")
public class AdminPasswordResetController {

    private final PasswordResetService passwordResetService;
    private final UserRepository userRepository;

    public AdminPasswordResetController(PasswordResetService passwordResetService,
                                        UserRepository userRepository) {
        this.passwordResetService = passwordResetService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ApiResult<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PasswordResetRequest> result = passwordResetService.list(status, page, size);
        List<Map<String, Object>> list = result.getContent().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", result.getTotalElements());
        data.put("pendingCount", passwordResetService.countPending());
        return ApiResult.ok(data);
    }

    @PostMapping("/{id}/issue-code")
    public ApiResult<Map<String, Object>> issueCode(@PathVariable Long id, @CurrentUser LoginUser admin) {
        PasswordResetService.IssuedCode issued = passwordResetService.issueCode(id, admin.userId());
        Map<String, Object> data = new HashMap<>();
        data.put("id", issued.id());
        data.put("phone", issued.phone());
        data.put("nickname", issued.nickname());
        data.put("code", issued.code());
        data.put("expireAt", issued.expireAt());
        return ApiResult.ok(data);
    }

    @PostMapping("/{id}/reject")
    public ApiResult<Void> reject(@PathVariable Long id, @CurrentUser LoginUser admin) {
        passwordResetService.reject(id, admin.userId());
        return ApiResult.ok();
    }

    private Map<String, Object> toMap(PasswordResetRequest r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("userId", r.getUserId());
        map.put("phone", r.getPhone());
        map.put("status", r.getStatus());
        map.put("requestIp", r.getRequestIp());
        map.put("codeExpireAt", r.getCodeExpireAt());
        map.put("handledAt", r.getHandledAt());
        map.put("createdAt", r.getCreatedAt());
        String nickname = userRepository.findById(r.getUserId()).map(User::getNickname).orElse(null);
        map.put("nickname", nickname);
        map.put("handledByName", r.getHandledBy() == null ? null
                : userRepository.findById(r.getHandledBy()).map(User::getNickname).orElse(null));
        return map;
    }
}
