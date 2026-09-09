package com.ielts.courses.play;

import com.ielts.courses.admin.AdminSecurityController;
import com.ielts.courses.auth.LoginUser;
import com.ielts.courses.catalog.Material;
import com.ielts.courses.catalog.MaterialRepository;
import com.ielts.courses.common.ApiException;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.common.BizCode;
import com.ielts.courses.common.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    private final MaterialRepository materialRepository;
    private final EntitlementService entitlementService;
    private final AdminSecurityController securityConfig;

    @Value("${app.tencent.cos.preview-url:}")
    private String previewUrlPrefix;

    public MaterialController(MaterialRepository materialRepository,
                              EntitlementService entitlementService,
                              AdminSecurityController securityConfig) {
        this.materialRepository = materialRepository;
        this.entitlementService = entitlementService;
        this.securityConfig = securityConfig;
    }

    @GetMapping("/{id}/preview-url")
    public ApiResult<Map<String, Object>> previewUrl(@CurrentUser LoginUser user,
                                                     @PathVariable Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ApiException(BizCode.NOT_FOUND));

        // 越权校验：仅已开通该课程的学员可获取课件地址，防止枚举 ID 下载付费课件
        if (user == null || !entitlementService.canPlayCourse(user.userId(), material.getCourseId())) {
            throw new ApiException(BizCode.NO_ENTITLEMENT);
        }

        // TODO: 接入腾讯云 COS 生成短时效签名 URL（存储桶转私有读后），目前返回公开拼链
        String url = previewUrlPrefix.isEmpty() ? "/preview/" + material.getCosKey() : previewUrlPrefix + material.getCosKey();
        return ApiResult.ok(Map.of(
                "url", url,
                "name", material.getName(),
                "type", material.getType(),
                "previewOnly", material.getPreviewOnly() != null && material.getPreviewOnly() == 1,
                "expireIn", securityConfig.getPreviewTtl()
        ));
    }
}
