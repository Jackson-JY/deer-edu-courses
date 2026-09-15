package com.ielts.courses.audit;

import com.ielts.courses.auth.LoginUser;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前端埋点上报（匿名可调；登录态由 JWT 过滤器自动识别）。
 * 仅接收页面浏览（PAGE_VIEW），事件类型在服务端固定，防止伪造任意事件。
 */
@RestController
@RequestMapping("/api/track")
public class TrackController {

    private final EventLogService eventLogService;

    public TrackController(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @PostMapping
    public ApiResult<Void> track(@CurrentUser LoginUser user,
                                 @Valid @RequestBody TrackReq req) {
        eventLogService.recordEvent(
                user == null ? null : user.userId(),
                "PAGE_VIEW", "PAGE", null, req.title(), req.path());
        return ApiResult.ok();
    }

    public record TrackReq(
            @NotBlank(message = "path 不能为空")
            @Size(max = 255)
            String path,
            @Size(max = 128)
            String title
    ) {
    }
}
