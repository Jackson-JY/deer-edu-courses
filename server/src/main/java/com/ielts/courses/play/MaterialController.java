package com.ielts.courses.play;

import com.ielts.courses.catalog.Material;
import com.ielts.courses.catalog.MaterialRepository;
import com.ielts.courses.common.ApiException;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.common.BizCode;
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

    @Value("${app.tencent.cos.preview-url:}")
    private String previewUrlPrefix;

    public MaterialController(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    @GetMapping("/{id}/preview-url")
    public ApiResult<Map<String, Object>> previewUrl(@PathVariable Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ApiException(BizCode.NOT_FOUND));
        // TODO: 接入腾讯云 COS 生成短时效签名 URL，目前返回 cosKey 拼接路径作为占位
        String url = previewUrlPrefix.isEmpty() ? "/preview/" + material.getCosKey() : previewUrlPrefix + material.getCosKey();
        return ApiResult.ok(Map.of(
                "url", url,
                "name", material.getName(),
                "type", material.getType(),
                "previewOnly", material.getPreviewOnly() != null && material.getPreviewOnly() == 1,
                "expireIn", 300
        ));
    }
}
