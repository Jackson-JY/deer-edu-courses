package com.ielts.courses.admin;

import com.ielts.courses.common.ApiResult;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/security")
public class AdminSecurityController {

    @Value("${app.watermark.template:{nickname} · {phoneTail} · UID{uid} · {time}}")
    private String watermarkTemplateDefault;

    @Value("${app.play.credential-ttl-seconds:300}")
    private long credentialTtlDefault;

    @Value("${app.play.preview-ttl-seconds:300}")
    private long previewTtlDefault;

    /** 运行时可被管理员修改的配置（重启后恢复为 application.yml 默认值） */
    private volatile String watermarkTemplate;
    private volatile long credentialTtl;
    private volatile long previewTtl;

    @PostConstruct
    void init() {
        this.watermarkTemplate = watermarkTemplateDefault;
        this.credentialTtl = credentialTtlDefault;
        this.previewTtl = previewTtlDefault;
    }

    @GetMapping
    public ApiResult<Map<String, Object>> getConfig() {
        return ApiResult.ok(Map.of(
                "watermarkTemplate", watermarkTemplate,
                "credentialTtl", credentialTtl,
                "previewTtl", previewTtl
        ));
    }

    @PutMapping
    public ApiResult<Map<String, Object>> updateConfig(@Valid @RequestBody SecurityConfigReq req) {
        this.watermarkTemplate = req.watermarkTemplate();
        this.credentialTtl = req.credentialTtl();
        this.previewTtl = req.previewTtl();
        return ApiResult.ok(Map.of(
                "watermarkTemplate", watermarkTemplate,
                "credentialTtl", credentialTtl,
                "previewTtl", previewTtl
        ));
    }

    /** 供 PlayService 读取运行时水印模板 */
    public String getWatermarkTemplate() {
        return watermarkTemplate;
    }

    public long getCredentialTtl() {
        return credentialTtl;
    }

    public long getPreviewTtl() {
        return previewTtl;
    }

    public record SecurityConfigReq(
            @NotBlank(message = "水印模板不能为空") String watermarkTemplate,
            @Min(value = 60, message = "播放凭证 TTL 至少 60 秒") long credentialTtl,
            @Min(value = 60, message = "课件预览 TTL 至少 60 秒") long previewTtl
    ) {
    }
}
