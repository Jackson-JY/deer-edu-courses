package com.ielts.courses.admin;

import com.ielts.courses.common.ApiResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/vod")
public class AdminVodController {

    @Value("${app.tencent.vod.app-id:0}")
    private long appId;

    @Value("${app.tencent.vod.sub-app-id:0}")
    private long subAppId;

    @Value("${app.tencent.vod.secret-id:}")
    private String secretId;

    @Value("${app.tencent.vod.secret-key:}")
    private String secretKey;

    @GetMapping("/upload-sign")
    public ApiResult<Map<String, Object>> uploadSign() {
        // TODO: 接入腾讯云 VOD 上传签名 SDK，生成客户端直传签名
        // 当前返回配置信息，前端可据此判断 VOD 是否已配置
        return ApiResult.ok(Map.of(
                "appId", appId,
                "subAppId", subAppId,
                "secretId", secretId.isEmpty() ? "" : secretId,
                "signature", "",
                "expireIn", 1800,
                "configured", appId > 0 && !secretId.isEmpty()
        ));
    }
}
