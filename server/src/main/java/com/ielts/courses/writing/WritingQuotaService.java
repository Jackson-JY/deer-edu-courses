package com.ielts.courses.writing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 写作批改额度：管理员开通后可用，按自然月（Asia/Shanghai）限 N 篇。
 */
@Service
public class WritingQuotaService {

    /** 与用户沟通确认：按自然月计，每月 3 篇（含 AI 生成失败的提交）。 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final WritingGrantRepository grantRepository;
    private final WritingSubmissionRepository submissionRepository;

    @Value("${app.writing.monthly-limit:3}")
    private int monthlyLimit;

    public WritingQuotaService(WritingGrantRepository grantRepository,
                               WritingSubmissionRepository submissionRepository) {
        this.grantRepository = grantRepository;
        this.submissionRepository = submissionRepository;
    }

    /** 额度快照：granted/expireAt/used/limit/remaining。 */
    public Map<String, Object> quota(Long userId) {
        WritingGrant grant = grantRepository.findByUserId(userId).orElse(null);
        boolean granted = grant != null && notExpired(grant);
        long used = usedThisMonth(userId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("granted", granted);
        m.put("expireAt", granted ? (grant.getExpireAt() == null ? null : grant.getExpireAt().toString()) : null);
        m.put("used", used);
        m.put("limit", monthlyLimit);
        m.put("remaining", Math.max(0, monthlyLimit - used));
        return m;
    }

    /** 校验可提交，不满足时抛带业务码的运行时异常（由全局处理器转 403）。 */
    public void checkCanSubmit(Long userId) {
        WritingGrant grant = grantRepository.findByUserId(userId).orElse(null);
        if (grant == null || !notExpired(grant)) {
            throw new WritingAccessDeniedException("写作批改未开通，请联系管理员开通后使用");
        }
        if (usedThisMonth(userId) >= monthlyLimit) {
            throw new WritingAccessDeniedException(
                    "本月提交额度已用完（" + monthlyLimit + "/" + monthlyLimit + "），下月自动恢复");
        }
    }

    private long usedThisMonth(Long userId) {
        LocalDateTime monthStart = LocalDate.now(ZONE).withDayOfMonth(1).atStartOfDay();
        return submissionRepository.countByUserIdAndCreatedAtAfter(userId, monthStart.minusNanos(1));
    }

    private static boolean notExpired(WritingGrant grant) {
        return grant.getExpireAt() == null || grant.getExpireAt().isAfter(LocalDateTime.now());
    }

    /** 权限类业务异常：转 HTTP 403 且不清登录态。 */
    public static class WritingAccessDeniedException extends RuntimeException {
        public WritingAccessDeniedException(String message) {
            super(message);
        }
    }
}
