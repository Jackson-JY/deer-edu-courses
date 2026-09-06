package com.ielts.courses.admin;

import com.ielts.courses.common.ApiResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/security")
public class AdminSecurityController {

    @Value("${app.watermark.template:{phoneTail} · UID{uid} · {time}}")
    private String watermarkTemplate;

    @Value("${app.play.credential-ttl-seconds:300}")
    private long credentialTtl;

    @Value("${app.play.preview-ttl-seconds:300}")
    private long previewTtl;

    @GetMapping
    public ApiResult<Map<String, Object>> getConfig() {
        return ApiResult.ok(Map.of(
                "watermarkTemplate", watermarkTemplate,
                "credentialTtl", credentialTtl,
                "previewTtl", previewTtl
        ));
    }
}
