package com.ielts.courses.play;

import com.ielts.courses.auth.LoginUser;
import com.ielts.courses.catalog.Lesson;
import com.ielts.courses.catalog.LessonRepository;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/play")
public class PlayController {

    private final PlayService playService;
    private final LessonRepository lessonRepository;

    public PlayController(PlayService playService, LessonRepository lessonRepository) {
        this.playService = playService;
        this.lessonRepository = lessonRepository;
    }

    @PostMapping("/credential")
    public ApiResult<PlayService.PlayCredential> credential(@CurrentUser LoginUser user,
                                                            @Valid @RequestBody CredentialReq req) {
        return ApiResult.ok(playService.issueCredential(user, req.lessonId));
    }

    @PostMapping("/heartbeat")
    public ApiResult<PlayService.HeartbeatResult> heartbeat(@CurrentUser LoginUser user,
                                                            @Valid @RequestBody HeartbeatReq req) {
        return ApiResult.ok(playService.heartbeat(user, req.lessonId, req.progressSeconds, req.totalSeconds));
    }

    @GetMapping("/progress")
    public ApiResult<List<Map<String, Object>>> progress(@CurrentUser LoginUser user) {
        List<WatchRecord> records = playService.getUserProgress(user.userId());
        if (records.isEmpty()) return ApiResult.ok(List.of());

        // 批量查课时标题，消除 N+1
        Set<Long> lessonIds = records.stream().map(WatchRecord::getLessonId).collect(Collectors.toSet());
        Map<Long, String> titleMap = lessonRepository.findAllById(lessonIds).stream()
                .collect(Collectors.toMap(Lesson::getId, Lesson::getTitle));

        return ApiResult.ok(records.stream()
                .map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("lessonId", r.getLessonId());
                    m.put("courseId", r.getCourseId());
                    m.put("lessonTitle", titleMap.getOrDefault(r.getLessonId(), ""));
                    m.put("progress", r.getProgress() != null ? r.getProgress() : 0);
                    m.put("lastPosition", r.getLastPosition() != null ? r.getLastPosition() : 0);
                    m.put("durationWatched", r.getDurationWatched() != null ? r.getDurationWatched() : 0);
                    m.put("finished", r.getFinished() != null ? r.getFinished() : 0);
                    m.put("updatedAt", r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : "");
                    return m;
                })
                .collect(Collectors.toList()));
    }

    public record CredentialReq(@NotNull Long lessonId) {
    }

    public record HeartbeatReq(@NotNull Long lessonId, @NotNull Integer progressSeconds, @NotNull Integer totalSeconds) {
    }
}
