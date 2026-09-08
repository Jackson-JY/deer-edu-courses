package com.ielts.courses.admin;

import com.ielts.courses.common.ApiException;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.common.BizCode;
import com.ielts.courses.play.Entitlement;
import com.ielts.courses.play.EntitlementRepository;
import com.ielts.courses.user.Staff;
import com.ielts.courses.user.StaffRepository;
import com.ielts.courses.user.User;
import com.ielts.courses.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final EntitlementRepository entitlementRepository;
    private final StaffRepository staffRepository;

    public AdminUserController(UserRepository userRepository, EntitlementRepository entitlementRepository,
                               StaffRepository staffRepository) {
        this.userRepository = userRepository;
        this.entitlementRepository = entitlementRepository;
        this.staffRepository = staffRepository;
    }

    @GetMapping
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) String phone) {
        List<User> users;
        if (phone != null && !phone.isEmpty()) {
            users = userRepository.findByPhone(phone).map(List::of).orElse(List.of());
        } else {
            users = userRepository.findAll();
        }
        List<Map<String, Object>> list = users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("phone", maskPhone(u.getPhone()));
            m.put("nickname", u.getNickname() == null ? "" : u.getNickname());
            m.put("status", u.getStatus());
            m.put("roles", staffRepository.findByUserIdAndStatus(u.getId(), 1).stream()
                    .map(Staff::getRole).collect(Collectors.toList()));
            return m;
        }).collect(Collectors.toList());
        return ApiResult.ok(list);
    }

    /** 分配角色：STUDENT / TEACHER / ADMIN */
    @PostMapping("/{userId}/role")
    public ApiResult<Map<String, Object>> assignRole(@PathVariable Long userId,
                                                     @Valid @RequestBody RoleReq req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(BizCode.NOT_FOUND, "用户不存在"));

        List<Staff> existing = staffRepository.findByUserIdAndStatus(userId, 1);
        Staff staff;
        if (existing.isEmpty()) {
            staff = new Staff();
            staff.setUserId(userId);
            staff.setStatus(1);
        } else {
            // 保留第一条作为主记录，其余置为失效，保证一个用户同时只有一条生效 staff 记录
            staff = existing.get(0);
            for (int i = 1; i < existing.size(); i++) {
                existing.get(i).setStatus(0);
                staffRepository.save(existing.get(i));
            }
        }
        staff.setRole(req.role());
        staffRepository.save(staff);

        Map<String, Object> m = new HashMap<>();
        m.put("userId", userId);
        m.put("role", req.role());
        return ApiResult.ok(m);
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

    public record RoleReq(
            @NotBlank(message = "角色不能为空")
            @Pattern(regexp = "STUDENT|TEACHER|ADMIN", message = "角色必须为 STUDENT/TEACHER/ADMIN")
            String role
    ) {
    }

    public record GrantReq(@NotNull Long courseId, String source, LocalDateTime expireAt) {
    }
}
