package com.ielts.courses.auth;

import com.ielts.courses.audit.EventLogService;
import com.ielts.courses.common.ApiException;
import com.ielts.courses.common.BizCode;
import com.ielts.courses.user.User;
import com.ielts.courses.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 找回密码：学生提交申请 → 管理员线下核实后签发一次性重置码 → 学生自助设置新密码。
 * 另含已登录用户修改密码。
 */
@Service
public class PasswordResetService {

    /** 同一手机号申请限流（24 小时一次） */
    private static final String REQ_LOCK_KEY = "pwdreset:req:";
    private static final Duration REQ_LOCK_TTL = Duration.ofHours(24);
    /** 重置码验证失败计数 */
    private static final String FAIL_KEY = "pwdreset:fail:";
    private static final int MAX_FAIL = 5;
    private static final Duration CODE_TTL = Duration.ofMinutes(30);
    /** 24 小时内已存在这些状态的申请时不再重复创建 */
    private static final List<String> ACTIVE_STATUSES =
            List.of(PasswordResetRequest.PENDING, PasswordResetRequest.CODE_ISSUED);

    private final PasswordResetRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final EventLogService eventLogService;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(PasswordResetRequestRepository requestRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                StringRedisTemplate redisTemplate,
                                EventLogService eventLogService) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.eventLogService = eventLogService;
    }

    /**
     * 学生提交找回密码申请。
     * 出于防枚举考虑：手机号不存在时也返回成功，不透露账号是否注册。
     */
    public void requestReset(String phone, String ip) {
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(REQ_LOCK_KEY + phone, "1", REQ_LOCK_TTL);
        if (Boolean.FALSE.equals(first)) {
            throw new ApiException(BizCode.BAD_REQUEST, "该手机号已提交过申请，请等待老师处理或 24 小时后再试");
        }

        User user = userRepository.findByPhone(phone).orElse(null);
        if (user == null) {
            // 不建申请，静默返回；前端文案保持统一
            return;
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            return; // 封禁账号不处理，同样静默
        }

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        if (requestRepository.existsByPhoneAndStatusInAndCreatedAtAfter(phone, ACTIVE_STATUSES, since)) {
            return;
        }

        PasswordResetRequest request = new PasswordResetRequest();
        request.setUserId(user.getId());
        request.setPhone(phone);
        request.setStatus(PasswordResetRequest.PENDING);
        request.setRequestIp(ip);
        requestRepository.save(request);

        eventLogService.recordEvent(user.getId(), "PASSWORD_RESET_REQUEST", "USER",
                String.valueOf(user.getId()), user.getNickname(), "提交找回密码申请");
    }

    /** 管理员签发一次性重置码，返回明文（仅此一次） */
    @Transactional
    public IssuedCode issueCode(Long requestId, Long adminUserId) {
        PasswordResetRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(BizCode.NOT_FOUND, "申请不存在"));
        if (PasswordResetRequest.USED.equals(request.getStatus())
                || PasswordResetRequest.REJECTED.equals(request.getStatus())) {
            throw new ApiException(BizCode.BAD_REQUEST, "该申请已处理，无法重复签发重置码");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expireAt = LocalDateTime.now().plus(CODE_TTL);
        request.setResetCodeHash(passwordEncoder.encode(code));
        request.setCodeExpireAt(expireAt);
        request.setStatus(PasswordResetRequest.CODE_ISSUED);
        request.setHandledBy(adminUserId);
        request.setHandledAt(LocalDateTime.now());
        requestRepository.save(request);

        String nickname = userRepository.findById(request.getUserId())
                .map(User::getNickname).orElse(null);
        return new IssuedCode(request.getId(), request.getPhone(), nickname, code, expireAt);
    }

    @Transactional
    public void reject(Long requestId, Long adminUserId) {
        PasswordResetRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(BizCode.NOT_FOUND, "申请不存在"));
        if (PasswordResetRequest.USED.equals(request.getStatus())) {
            throw new ApiException(BizCode.BAD_REQUEST, "该申请已完成密码重置，无法拒绝");
        }
        request.setStatus(PasswordResetRequest.REJECTED);
        request.setHandledBy(adminUserId);
        request.setHandledAt(LocalDateTime.now());
        request.setResetCodeHash(null);
        requestRepository.save(request);
    }

    /** 学生凭手机号 + 重置码设置新密码 */
    @Transactional
    public void confirmReset(String phone, String code, String newPassword) {
        PasswordResetRequest request = requestRepository
                .findFirstByPhoneAndStatusOrderByIdDesc(phone, PasswordResetRequest.CODE_ISSUED)
                .orElseThrow(() -> new ApiException(BizCode.BAD_REQUEST, "没有可用的重置码，请先提交申请并联系老师获取"));

        if (request.getCodeExpireAt() == null || request.getCodeExpireAt().isBefore(LocalDateTime.now())) {
            request.setStatus(PasswordResetRequest.EXPIRED);
            requestRepository.save(request);
            throw new ApiException(BizCode.BAD_REQUEST, "重置码已过期，请重新提交申请");
        }

        String failKey = FAIL_KEY + request.getId();
        String failCount = redisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= MAX_FAIL) {
            throw new ApiException(BizCode.BAD_REQUEST, "验证失败次数过多，请联系老师重新获取重置码");
        }

        if (request.getResetCodeHash() == null
                || !passwordEncoder.matches(code, request.getResetCodeHash())) {
            Long count = redisTemplate.opsForValue().increment(failKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(failKey, CODE_TTL);
            }
            throw new ApiException(BizCode.BAD_REQUEST, "重置码不正确");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ApiException(BizCode.NOT_FOUND, "用户不存在"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        request.setStatus(PasswordResetRequest.USED);
        requestRepository.save(request);
        redisTemplate.delete(failKey);
        // 强制下线所有已登录设备
        redisTemplate.delete("login:session:" + user.getId());

        eventLogService.recordEvent(user.getId(), "PASSWORD_RESET", "USER",
                String.valueOf(user.getId()), user.getNickname(), "通过重置码找回密码");
    }

    /** 已登录用户修改密码：校验原密码，成功后保持当前会话 */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(BizCode.NOT_FOUND, "用户不存在"));
        if (oldPassword == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new ApiException(BizCode.BAD_REQUEST, "原密码不正确");
        }
        if (oldPassword.equals(newPassword)) {
            throw new ApiException(BizCode.BAD_REQUEST, "新密码不能与原密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        eventLogService.recordEvent(userId, "PASSWORD_CHANGE", "USER",
                String.valueOf(userId), user.getNickname(), "用户自助修改密码");
    }

    @Transactional(readOnly = true)
    public Page<PasswordResetRequest> list(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        if (status == null || status.isBlank()) {
            return requestRepository.findAllByOrderByIdDesc(pageable);
        }
        return requestRepository.findAllByStatusOrderByIdDesc(status.toUpperCase(), pageable);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return requestRepository.countByStatus(PasswordResetRequest.PENDING);
    }

    public record IssuedCode(Long id, String phone, String nickname, String code, LocalDateTime expireAt) {
    }
}
