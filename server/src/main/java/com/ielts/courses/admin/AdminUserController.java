package com.ielts.courses.admin;

import com.ielts.courses.catalog.Course;
import com.ielts.courses.catalog.CourseRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
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
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final EntitlementRepository entitlementRepository;
    private final StaffRepository staffRepository;
    private final CourseRepository courseRepository;
    private final com.ielts.courses.writing.WritingGrantRepository writingGrantRepository;

    public AdminUserController(UserRepository userRepository, EntitlementRepository entitlementRepository,
                               StaffRepository staffRepository, CourseRepository courseRepository,
                               com.ielts.courses.writing.WritingGrantRepository writingGrantRepository) {
        this.userRepository = userRepository;
        this.entitlementRepository = entitlementRepository;
        this.staffRepository = staffRepository;
        this.courseRepository = courseRepository;
        this.writingGrantRepository = writingGrantRepository;
    }

    @GetMapping
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) String phone) {
        List<User> users;
        if (phone != null && !phone.isEmpty()) {
            users = userRepository.findByPhone(phone).map(List::of).orElse(List.of());
        } else {
            users = userRepository.findAll();
        }
        // 批量取写作权限，避免逐条查询
        Map<Long, com.ielts.courses.writing.WritingGrant> grantMap = new HashMap<>();
        for (com.ielts.courses.writing.WritingGrant g : writingGrantRepository.findAllBy()) {
            grantMap.put(g.getUserId(), g);
        }
        List<Map<String, Object>> list = users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("phone", maskPhone(u.getPhone()));
            m.put("nickname", u.getNickname() == null ? "" : u.getNickname());
            m.put("status", u.getStatus());
            m.put("roles", staffRepository.findByUserIdAndStatus(u.getId(), 1).stream()
                    .map(Staff::getRole).collect(Collectors.toList()));
            com.ielts.courses.writing.WritingGrant grant = grantMap.get(u.getId());
            boolean grantActive = grant != null
                    && (grant.getExpireAt() == null || grant.getExpireAt().isAfter(LocalDateTime.now()));
            m.put("writingGranted", grantActive);
            m.put("writingExpireAt", grantActive && grant.getExpireAt() != null ? grant.getExpireAt().toString() : null);
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

    /** 查询指定用户已开通的全部权益（含课程标题） */
    @GetMapping("/{userId}/entitlements")
    public ApiResult<List<Map<String, Object>>> listEntitlements(@PathVariable Long userId) {
        List<Entitlement> ents = entitlementRepository.findByUserIdAndStatus(userId, "ACTIVE");
        // 批量查询课程标题，避免逐条 findById 的 N+1
        Set<Long> courseIds = ents.stream().map(Entitlement::getCourseId).collect(Collectors.toSet());
        Map<Long, String> titleMap = courseIds.isEmpty() ? Map.of()
                : courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, c -> c.getTitle() == null ? "未命名课程" : c.getTitle()));
        return ApiResult.ok(ents.stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId());
            m.put("courseId", e.getCourseId());
            m.put("title", titleMap.getOrDefault(e.getCourseId(), "未知课程"));
            m.put("source", e.getSource());
            m.put("expireAt", e.getExpireAt() == null ? "" : e.getExpireAt().toString());
            m.put("status", e.getStatus());
            return m;
        }).collect(Collectors.toList()));
    }

    /** 开通课程权益（幂等：已存在 ACTIVE 权益则更新到期时间，避免重复键 500） */
    @PostMapping("/{userId}/entitlements")
    public ApiResult<Entitlement> grant(@PathVariable Long userId,
                                        @Valid @RequestBody GrantReq req) {
        userRepository.findById(userId).orElseThrow(() -> new ApiException(BizCode.NOT_FOUND, "用户不存在"));
        courseRepository.findById(req.courseId()).orElseThrow(() -> new ApiException(BizCode.NOT_FOUND, "课程不存在"));
        LocalDateTime expire = parseExpireAt(req.expireAt());

        Entitlement existing = entitlementRepository.findByUserIdAndCourseIdAndStatus(userId, req.courseId(), "ACTIVE").orElse(null);
        if (existing != null) {
            // 已开通则更新到期时间（新指定的覆盖原时间；留空表示改永久）
            existing.setExpireAt(expire);
            return ApiResult.ok(entitlementRepository.save(existing));
        }
        Entitlement entitlement = new Entitlement();
        entitlement.setUserId(userId);
        entitlement.setCourseId(req.courseId());
        entitlement.setSource(req.source() == null ? "MANUAL" : req.source());
        entitlement.setExpireAt(expire);
        entitlement.setStatus("ACTIVE");
        try {
            return ApiResult.ok(entitlementRepository.save(entitlement));
        } catch (DataIntegrityViolationException e) {
            // 并发场景下重复提交：再查一次返回已存在记录
            Entitlement dup = entitlementRepository.findByUserIdAndCourseIdAndStatus(userId, req.courseId(), "ACTIVE").orElse(null);
            if (dup != null) {
                dup.setExpireAt(expire);
                return ApiResult.ok(entitlementRepository.save(dup));
            }
            throw e;
        }
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

    public record GrantReq(@NotNull Long courseId, String source, String expireAt) {
    }

    /** 解析到期时间，兼容前端 "yyyy-MM-dd HH:mm:ss" 与 ISO "yyyy-MM-dd'T'HH:mm:ss" 两种格式 */
    private LocalDateTime parseExpireAt(String expireAt) {
        if (expireAt == null || expireAt.isBlank()) return null;
        String normalized = expireAt.trim().replace(' ', 'T');
        return LocalDateTime.parse(normalized);
    }
}
