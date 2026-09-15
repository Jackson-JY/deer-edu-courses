package com.ielts.courses.audit;

import com.ielts.courses.user.User;
import com.ielts.courses.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审计日志写入服务。
 * 所有写库均以独立事务执行，且整体 try/catch 吞异常——
 * 审计失败绝不允许影响正常业务请求。
 */
@Service
public class EventLogService {

    private static final Logger log = LoggerFactory.getLogger(EventLogService.class);

    private final AdminOperationLogRepository adminRepo;
    private final UserEventLogRepository eventRepo;
    private final UserRepository userRepository;

    public EventLogService(AdminOperationLogRepository adminRepo,
                           UserEventLogRepository eventRepo,
                           UserRepository userRepository) {
        this.adminRepo = adminRepo;
        this.eventRepo = eventRepo;
        this.userRepository = userRepository;
    }

    /** 管理员写操作日志（AOP 切面调用） */
    void saveAdminOperation(AdminOperationLog entry) {
        try {
            saveAdminInNewTx(entry);
        } catch (Exception e) {
            log.warn("写入管理员操作日志失败: {}", e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAdminInNewTx(AdminOperationLog entry) {
        adminRepo.save(entry);
    }

    /** 认证模块显式记录：管理员登录成功/失败（登录接口不在 admin 包，AOP 覆盖不到） */
    public void recordAdminLogin(Long userId, String name, boolean success, String error) {
        AdminOperationLog entry = new AdminOperationLog();
        entry.setAdminUserId(userId);
        entry.setAdminName(WebUtils.truncate(name, 64));
        entry.setModule("auth");
        entry.setAction(success ? "管理员登录" : "管理员登录失败");
        entry.setMethod("POST");
        entry.setPath("/api/auth/login");
        HttpServletRequest req = WebUtils.currentRequest();
        entry.setIp(WebUtils.clientIp(req));
        entry.setUserAgent(WebUtils.userAgent(req));
        entry.setSuccess(success ? 1 : 0);
        entry.setError(WebUtils.truncate(error, 500));
        saveAdminOperation(entry);
    }

    /**
     * 记录用户事件。userId 可为 null（匿名浏览）。
     */
    public void recordEvent(Long userId, String eventType, String targetType,
                            String targetId, String targetName, String detail) {
        try {
            UserEventLog entry = new UserEventLog();
            entry.setUserId(userId);
            entry.setEventType(eventType);
            entry.setTargetType(targetType);
            entry.setTargetId(WebUtils.truncate(targetId, 64));
            entry.setTargetName(WebUtils.truncate(targetName, 255));
            entry.setDetail(WebUtils.truncate(detail, 500));
            HttpServletRequest req = WebUtils.currentRequest();
            entry.setIp(WebUtils.clientIp(req));
            entry.setUserAgent(WebUtils.userAgent(req));
            saveEventInNewTx(entry);
        } catch (Exception e) {
            log.warn("写入用户事件日志失败: {}", e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveEventInNewTx(UserEventLog entry) {
        eventRepo.save(entry);
    }

    /** 操作者展示名快照：昵称优先，否则手机号，否则 UID */
    public String resolveDisplayName(Long userId) {
        if (userId == null) return null;
        try {
            User u = userRepository.findById(userId).orElse(null);
            if (u == null) return "UID" + userId;
            if (u.getNickname() != null && !u.getNickname().isBlank()) {
                return u.getNickname();
            }
            return u.getPhone() != null ? u.getPhone() : "UID" + userId;
        } catch (Exception e) {
            return "UID" + userId;
        }
    }
}
