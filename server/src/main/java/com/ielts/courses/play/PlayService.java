package com.ielts.courses.play;

import com.ielts.courses.auth.LoginUser;
import com.ielts.courses.catalog.Lesson;
import com.ielts.courses.catalog.LessonRepository;
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

    @Value("${app.play.credential-ttl-seconds:300}")
    private long credentialTtl;

    @Value("${app.play.watermark-template:{phoneTail} · UID{uid} · {time}}")
    private String watermarkTemplate;

    public PlayService(LessonRepository lessonRepository, EntitlementService entitlementService,
                       WatchRecordRepository watchRecordRepository, VodPsignGenerator psignGenerator,
                       StringRedisTemplate redisTemplate, UserRepository userRepository) {
        this.lessonRepository = lessonRepository;
        this.entitlementService = entitlementService;
        this.watchRecordRepository = watchRecordRepository;
        this.psignGenerator = psignGenerator;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
    }

    public PlayCredential issueCredential(LoginUser user, long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException(BizCode.NOT_FOUND));

        if (!entitlementService.canPlay(user.userId(), lessonId)) {
            throw new ApiException(BizCode.NO_ENTITLEMENT);
        }

        verifyActiveSession(user);

        String watermarkText = buildWatermarkText(user);
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
                    return r;
                });
        record.setProgress(Math.max(record.getProgress(), progressSeconds));
        record.setLastPosition(Math.max(record.getLastPosition(), progressSeconds));
        record.setDurationWatched(Math.max(record.getDurationWatched(), progressSeconds));
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

    private String buildWatermarkText(LoginUser user) {
        // 手机尾号：取手机号后 4 位；取不到时回退为 UID
        String phoneTail = userRepository.findById(user.userId())
                .map(User::getPhone)
                .filter(p -> p != null && p.length() >= 4)
                .map(p -> p.substring(p.length() - 4))
                .orElse("UID" + user.userId());
        return watermarkTemplate
                .replace("{phoneTail}", phoneTail)
                .replace("{uid}", String.valueOf(user.userId()))
                .replace("{time}", LocalDateTime.now().format(WM_TIME_FMT));
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
