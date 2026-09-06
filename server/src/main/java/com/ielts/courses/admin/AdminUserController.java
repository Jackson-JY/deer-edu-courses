package com.ielts.courses.admin;

import com.ielts.courses.common.ApiResult;
import com.ielts.courses.play.Entitlement;
import com.ielts.courses.play.EntitlementRepository;
import com.ielts.courses.user.User;
import com.ielts.courses.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final EntitlementRepository entitlementRepository;

    public AdminUserController(UserRepository userRepository, EntitlementRepository entitlementRepository) {
        this.userRepository = userRepository;
        this.entitlementRepository = entitlementRepository;
    }

    @GetMapping
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) String phone) {
        List<User> users;
        if (phone != null && !phone.isEmpty()) {
            users = userRepository.findByPhone(phone).map(List::of).orElse(List.of());
        } else {
            users = userRepository.findAll();
        }
        List<Map<String, Object>> list = users.stream().map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "phone", maskPhone(u.getPhone()),
                "nickname", u.getNickname() == null ? "" : u.getNickname(),
                "status", u.getStatus()
        )).collect(Collectors.toList());
        return ApiResult.ok(list);
    }

    @PostMapping("/{userId}/entitlements")
    public ApiResult<Entitlement> grant(@PathVariable Long userId,
                                        @Valid @RequestBody GrantReq req) {
        User user = userRepository.findById(userId).orElseThrow();
        Entitlement entitlement = new Entitlement();
        entitlement.setUserId(userId);
        entitlement.setCourseId(req.courseId());
        entitlement.setSource(req.source() == null ? "MANUAL" : req.source());
        entitlement.setExpireAt(req.expireAt());
        entitlement.setStatus("ACTIVE");
        return ApiResult.ok(entitlementRepository.save(entitlement));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    public record GrantReq(@NotNull Long courseId, String source, LocalDateTime expireAt) {
    }
}
