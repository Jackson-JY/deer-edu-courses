package com.ielts.courses.play;

import com.ielts.courses.auth.LoginUser;
import com.ielts.courses.catalog.Lesson;
import com.ielts.courses.catalog.LessonRepository;
import com.ielts.courses.admin.AdminSecurityController;
import com.ielts.courses.common.ApiException;
import com.ielts.courses.common.BizCode;
import com.ielts.courses.user.User;
import com.ielts.courses.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PlayService {

    private static final DateTimeFormatter WM_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final LessonRepository lessonRepository;
    private final EntitlementService entitlementService;
    private final WatchRecordRepository watchRecordRepository;
    private final VodPsignGenerator psignGenerator;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    @Value("${app.tencent.vod.app-id:0}")
    private long vodAppId;

    @Value("${app.tencent.vod.play-key:}")
    private String vodPlayKey;

    private final AdminSecurityController securityConfig;

    public PlayService(LessonRepository lessonRepository, EntitlementService entitlementService,
                       WatchRecordRepository watchRecordRepository, VodPsignGenerator psignGenerator,
                       StringRedisTemplate redisTemplate, UserRepository userRepository,
                       AdminSecurityController securityConfig) {
        this.lessonRepository = lessonRepository;
        this.entitlementService = entitlementService;
        this.watchRecordRepository = watchRecordRepository;
        this.psignGenerator = psignGenerator;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.securityConfig = securityConfig;
    }

    public PlayCredential issueCredential(LoginUser user, long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException(BizCode.NOT_FOUND));

        if (!entitlementService.canPlay(user.userId(), lessonId)) {
            throw new ApiException(BizCode.NO_ENTITLEMENT);
        }

        verifyActiveSession(user);

        long credentialTtl = securityConfig.getCredentialTtl();
        String watermarkText = buildWatermarkText(user, lesson.getTitle());
        String psign = null;
        if (vodAppId > 0 && vodPlayKey != null && !vodPlayKey.isEmpty()) {
            psign = psignGenerator.generate(vodAppId, lesson.getVodFileId(), vodPlayKey, credentialTtl, watermarkText);
        }

        int lastPosition = findLastPosition(user.userId(), lessonId);
        boolean isTrial = entitlementService.isFreeTrial(lesson);

        return new PlayCredential(
                lesson.getVodFileId(),
                psign,
                vodAppId,
                credentialTtl,
                lastPosition,
                isTrial,
                new Watermark(watermarkText, 0.12, "MARQUEE")
        );
    }

    public java.util.List<WatchRecord> getUserProgress(long userId) {
        return watchRecordRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public HeartbeatResult heartbeat(LoginUser user, long lessonId, int progressSeconds, int totalSeconds) {
        verifyActiveSession(user);

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException(BizCode.NOT_FOUND));

        if (!entitlementService.canPlay(user.userId(), lessonId)) {
            throw new ApiException(BizCode.NO_ENTITLEMENT);
        }

        WatchRecord record = watchRecordRepository.findByUserIdAndLessonId(user.userId(), lessonId)
                .orElseGet(() -> {
                    WatchRecord r = new WatchRecord();
                    r.setUserId(user.userId());
                    r.setCourseId(lesson.getCourseId());
                    r.setLessonId(lessonId);
                    r.setDurationWatched(0);
                    return r;
                });
        // durationWatched 按心跳间隔累加：如果本次上报进度比上次前进不超过 35s（心跳间隔 30s + 容差），
        // 说明在正常播放，累加增量；如果后退或前进过多（拖拽），不累加
        int prevPosition = record.getLastPosition() != null ? record.getLastPosition() : 0;
        int delta = progressSeconds - prevPosition;
        if (delta > 0 && delta <= 35) {
            int watched = record.getDurationWatched() != null ? record.getDurationWatched() : 0;
            record.setDurationWatched(watched + delta);
        }

        record.setProgress(Math.max(record.getProgress(), progressSeconds));
        record.setLastPosition(Math.max(record.getLastPosition(), progressSeconds));

        if (totalSeconds > 0 && progressSeconds >= totalSeconds * 0.9) {
            record.setFinished(1);
        }
        watchRecordRepository.save(record);

        return new HeartbeatResult(true, record.getProgress());
    }

    private void verifyActiveSession(LoginUser user) {
        String activeSid = redisTemplate.opsForValue().get("login:session:" + user.userId());
        if (!user.sid().equals(activeSid)) {
            throw new ApiException(BizCode.SESSION_KICKED);
        }
    }

    /**
     * 构建动态水印文案，支持以下占位符（在 app.play.watermark-template 中配置）：
     *   {nickname}  用户昵称/用户名（缺省回退“学员”，超长截断）
     *   {phone}     脱敏完整手机号，如 138****8000
     *   {phoneTail} 手机号后 4 位
     *   {uid}       用户 ID
     *   {time}      当前时间 yyyy-MM-dd HH:mm
     *   {lesson}    当前课时名称
     */
    private String buildWatermarkText(LoginUser user, String lessonTitle) {
        User u = userRepository.findById(user.userId()).orElse(null);
        String phone = u != null ? u.getPhone() : null;

        String phoneTail = (phone != null && phone.length() >= 4)
                ? phone.substring(phone.length() - 4) : "****";
        String maskedPhone = (phone != null && phone.length() >= 7)
                ? phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4)
                : phoneTail;
        String nickname = (u != null && u.getNickname() != null && !u.getNickname().isBlank())
                ? truncate(u.getNickname().trim(), 8) : "学员";
        String lesson = lessonTitle == null ? "" : truncate(lessonTitle.trim(), 12);

        return securityConfig.getWatermarkTemplate()
                .replace("{nickname}", nickname)
                .replace("{phone}", maskedPhone)
                .replace("{phoneTail}", phoneTail)
                .replace("{lesson}", lesson)
                .replace("{uid}", String.valueOf(user.userId()))
                .replace("{time}", LocalDateTime.now().format(WM_TIME_FMT));
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private int findLastPosition(long userId, long lessonId) {
        return watchRecordRepository.findByUserIdAndLessonId(userId, lessonId)
                .map(WatchRecord::getLastPosition)
                .orElse(0);
    }

    public record PlayCredential(String vodFileId, String psign, long appId, long expireIn,
                                 int lastPosition, boolean isTrial, Watermark watermark) {
    }

    public record Watermark(String text, double opacity, String mode) {
    }

    public record HeartbeatResult(boolean alive, int progressSeconds) {
    }
}
