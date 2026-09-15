package com.ielts.courses.admin;

import com.ielts.courses.audit.AdminOperationLog;
import com.ielts.courses.audit.AdminOperationLogRepository;
import com.ielts.courses.audit.UserEventLog;
import com.ielts.courses.audit.UserEventLogRepository;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.user.User;
import com.ielts.courses.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 审计日志查询 + 访问看板数据。
 */
@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogController {

    private final AdminOperationLogRepository adminLogRepository;
    private final UserEventLogRepository eventLogRepository;
    private final UserRepository userRepository;

    public AdminLogController(AdminOperationLogRepository adminLogRepository,
                              UserEventLogRepository eventLogRepository,
                              UserRepository userRepository) {
        this.adminLogRepository = adminLogRepository;
        this.eventLogRepository = eventLogRepository;
        this.userRepository = userRepository;
    }

    /** 管理员操作日志（分页，可按管理员/模块/关键词过滤） */
    @GetMapping("/admin")
    public ApiResult<Map<String, Object>> adminLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String keyword) {
        Page<AdminOperationLog> result = adminLogRepository.search(
                adminId, blankToNull(module), blankToNull(keyword), pageable(page, size));
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent().stream().map(this::toAdminMap).toList());
        data.put("total", result.getTotalElements());
        return ApiResult.ok(data);
    }

    /** 用户事件日志（分页，可按用户/事件类型过滤） */
    @GetMapping("/user")
    public ApiResult<Map<String, Object>> userLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String targetType) {
        Page<UserEventLog> result = eventLogRepository.search(
                userId, blankToNull(eventType), blankToNull(targetType), pageable(page, size));
        Map<Long, String> nameMap = loadUserNames(result.getContent().stream()
                .map(UserEventLog::getUserId).collect(Collectors.toSet()));
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent().stream()
                .map(e -> toEventMap(e, nameMap.get(e.getUserId()))).toList());
        data.put("total", result.getTotalElements());
        return ApiResult.ok(data);
    }

    /** 访问看板概览：今日指标 + 近 14 天趋势 + 课程浏览榜 + 最近动态 */
    @GetMapping("/overview")
    public ApiResult<Map<String, Object>> overview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime since14 = LocalDate.now().minusDays(13).atStartOfDay();

        Map<String, Object> data = new HashMap<>();
        data.put("todayPv", eventLogRepository.countPvSince(todayStart));
        data.put("todayUv", eventLogRepository.countUvSince(todayStart));
        data.put("todayAdminOps", adminLogRepository.countByCreatedAtAfter(todayStart));
        data.put("pvTrend", buildPvTrend(since14));
        data.put("topCourses", buildTopCourses(since14));

        // 今日各事件类型计数
        Map<String, Long> typeCounts = new HashMap<>();
        for (Object[] row : eventLogRepository.countByTypeSince(todayStart)) {
            typeCounts.put((String) row[0], ((Number) row[1]).longValue());
        }
        data.put("eventTypeCounts", typeCounts);

        // 最近管理员操作
        data.put("recentAdminOps", adminLogRepository.findTop10ByOrderByIdDesc().stream()
                .map(this::toAdminMap).toList());

        // 最近用户动态（带用户名）
        List<UserEventLog> recentEvents = eventLogRepository.findTop20ByOrderByIdDesc();
        Map<Long, String> nameMap = loadUserNames(recentEvents.stream()
                .map(UserEventLog::getUserId).collect(Collectors.toSet()));
        data.put("recentUserEvents", recentEvents.stream()
                .map(e -> toEventMap(e, nameMap.get(e.getUserId()))).toList());
        return ApiResult.ok(data);
    }

    private List<Map<String, Object>> buildPvTrend(LocalDateTime since) {
        Map<LocalDate, long[]> byDate = new HashMap<>();
        for (Object[] row : eventLogRepository.dailyPvUv(since)) {
            LocalDate d = ((java.sql.Date) row[0]).toLocalDate();
            byDate.put(d, new long[]{((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            LocalDate d = LocalDate.now().minusDays(13 - i);
            long[] v = byDate.get(d);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", d.toString());
            m.put("pv", v == null ? 0 : v[0]);
            m.put("uv", v == null ? 0 : v[1]);
            trend.add(m);
        }
        return trend;
    }

    private List<Map<String, Object>> buildTopCourses(LocalDateTime since) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : eventLogRepository.topCourses(since)) {
            Map<String, Object> m = new HashMap<>();
            m.put("courseId", row[0]);
            m.put("title", row[1] == null ? "课程 " + row[0] : row[1]);
            m.put("views", ((Number) row[2]).longValue());
            list.add(m);
        }
        return list;
    }

    private Map<String, Object> toAdminMap(AdminOperationLog l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("adminUserId", l.getAdminUserId());
        m.put("adminName", l.getAdminName());
        m.put("module", l.getModule());
        m.put("action", l.getAction());
        m.put("method", l.getMethod());
        m.put("path", l.getPath());
        m.put("params", l.getParams());
        m.put("ip", l.getIp());
        m.put("userAgent", l.getUserAgent());
        m.put("success", l.getSuccess());
        m.put("error", l.getError());
        m.put("durationMs", l.getDurationMs());
        m.put("createdAt", l.getCreatedAt() == null ? null : l.getCreatedAt().toString());
        return m;
    }

    private Map<String, Object> toEventMap(UserEventLog e, String userName) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("userId", e.getUserId());
        m.put("userName", userName);
        m.put("eventType", e.getEventType());
        m.put("targetType", e.getTargetType());
        m.put("targetId", e.getTargetId());
        m.put("targetName", e.getTargetName());
        m.put("detail", e.getDetail());
        m.put("ip", e.getIp());
        m.put("userAgent", e.getUserAgent());
        m.put("createdAt", e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
        return m;
    }

    private Map<Long, String> loadUserNames(Set<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId,
                        u -> u.getNickname() != null && !u.getNickname().isBlank()
                                ? u.getNickname() : maskPhone(u.getPhone()),
                        (a, b) -> a));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone == null ? "" : phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private PageRequest pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage, safeSize);
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
