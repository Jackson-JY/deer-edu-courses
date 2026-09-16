package com.ielts.courses.writing;

import com.ielts.courses.common.ApiException;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.common.BizCode;
import com.ielts.courses.user.User;
import com.ielts.courses.user.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 管理员：写作批改权限开通/撤销/列表（ADMIN 角色）。 */
@RestController
@RequestMapping("/api/admin/writing")
public class WritingAdminController {

    private final WritingGrantRepository grantRepository;
    private final WritingQuotaService quotaService;
    private final UserRepository userRepository;

    public WritingAdminController(WritingGrantRepository grantRepository,
                                  WritingQuotaService quotaService,
                                  UserRepository userRepository) {
        this.grantRepository = grantRepository;
        this.quotaService = quotaService;
        this.userRepository = userRepository;
    }

    public record GrantReq(String expireAt) {
    }

    /** 已开通写作权限的用户列表（含本月用量）。 */
    @GetMapping("/grants")
    @Transactional(readOnly = true)
    public ApiResult<List<Map<String, Object>>> list() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (WritingGrant g : grantRepository.findAllBy()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", g.getUserId());
            m.put("expireAt", g.getExpireAt() == null ? null : g.getExpireAt().toString());
            m.put("active", g.getExpireAt() == null || g.getExpireAt().isAfter(LocalDateTime.now()));
            userRepository.findById(g.getUserId()).ifPresent(u -> {
                m.put("nickname", u.getNickname());
                m.put("phone", maskPhone(u.getPhone()));
            });
            @SuppressWarnings("unchecked")
            Map<String, Object> q = quotaService.quota(g.getUserId());
            m.put("used", q.get("used"));
            m.put("limit", q.get("limit"));
            rows.add(m);
        }
        return ApiResult.ok(rows);
    }

    /** 开通写作权限（幂等：重复开通更新到期时间；expireAt 空 = 永久）。 */
    @PostMapping("/users/{userId}/grant")
    @Transactional
    public ApiResult<Map<String, Object>> grant(@PathVariable Long userId,
                                                @RequestBody(required = false) GrantReq req) {
        ensureUser(userId);
        WritingGrant grant = grantRepository.findByUserId(userId).orElseGet(WritingGrant::new);
        grant.setUserId(userId);
        grant.setExpireAt(parseExpireAt(req == null ? null : req.expireAt()));
        grant = grantRepository.save(grant);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", userId);
        m.put("expireAt", grant.getExpireAt() == null ? null : grant.getExpireAt().toString());
        return ApiResult.ok(m);
    }

    /** 撤销写作权限。 */
    @DeleteMapping("/users/{userId}/grant")
    @Transactional
    public ApiResult<Void> revoke(@PathVariable Long userId) {
        grantRepository.findByUserId(userId).ifPresent(grantRepository::delete);
        return ApiResult.ok();
    }

    private void ensureUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(BizCode.NOT_FOUND, "用户不存在"));
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private static LocalDateTime parseExpireAt(String expireAt) {
        if (expireAt == null || expireAt.isBlank()) return null;
        String normalized = expireAt.trim().replace(' ', 'T');
        return LocalDateTime.parse(normalized);
    }
}
