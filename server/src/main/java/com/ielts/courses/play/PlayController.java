package com.ielts.courses.play;

import com.ielts.courses.auth.LoginUser;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/play")
public class PlayController {

    private final PlayService playService;

    public PlayController(PlayService playService) {
        this.playService = playService;
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

    public record CredentialReq(@NotNull Long lessonId) {
    }

    public record HeartbeatReq(@NotNull Long lessonId, @NotNull Integer progressSeconds, @NotNull Integer totalSeconds) {
    }
}
