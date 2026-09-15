package com.ielts.courses.admin;

import com.ielts.courses.common.ApiException;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.common.BizCode;
import com.ielts.courses.play.VodMediaStatusService;
import com.ielts.courses.play.VodOpenApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

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

    @Value("${app.tencent.vod.adaptive-definition:10}")
    private int adaptiveDefinition;

    @Value("${app.tencent.vod.upload-procedure:}")
    private String uploadProcedure;

    private final VodOpenApiClient vodOpenApiClient;
    private final VodMediaStatusService mediaStatusService;

    public AdminVodController(VodOpenApiClient vodOpenApiClient, VodMediaStatusService mediaStatusService) {
        this.vodOpenApiClient = vodOpenApiClient;
        this.mediaStatusService = mediaStatusService;
    }

    @GetMapping("/upload-sign")
    public ApiResult<Map<String, Object>> uploadSign() {
        boolean configured = appId > 0 && !secretId.isEmpty() && !secretKey.isEmpty();
        String signature = "";
        long expire = 0;
        if (configured) {
            long current = System.currentTimeMillis() / 1000;
            expire = current + 3600;
            signature = generateClientUploadSignature(current, expire);
        }
        return ApiResult.ok(Map.of(
                "appId", appId,
                "subAppId", subAppId,
                "secretId", secretId,
                "signature", signature,
                "expireTime", expire,
                "configured", configured
        ));
    }

    /**
     * 对存量视频发起转自适应码流任务（模板取配置，默认预置 Adaptive-HLS 10）。
     * 转码完成后播放凭证会自动切换为 HLS（状态查询带缓存，本接口会主动清缓存）。
     */
    @PostMapping("/files/{fileId}/transcode")
    public ApiResult<Map<String, Object>> transcode(@PathVariable String fileId,
                                                    @RequestParam(required = false) Integer definition) {
        int def = definition != null ? definition : adaptiveDefinition;
        if (def <= 0) {
            throw new ApiException(BizCode.BAD_REQUEST, "自适应码流模板 ID 未配置");
        }
        try {
            String taskId = vodOpenApiClient.processAdaptive(fileId, def);
            // 立即清掉"未转码"缓存，转码完成后的下一次凭证请求即可拿到最新状态
            mediaStatusService.evict(fileId);
            return ApiResult.ok(Map.of("fileId", fileId, "definition", def, "taskId", taskId == null ? "" : taskId));
        } catch (Exception e) {
            throw new ApiException(BizCode.BAD_REQUEST, "发起转码失败：" + e.getMessage());
        }
    }

    /** 查询视频当前是否已有可播放的自适应码流输出（实时查询并刷新缓存） */
    @GetMapping("/files/{fileId}/adaptive")
    public ApiResult<Map<String, Object>> adaptiveStatus(@PathVariable String fileId) {
        mediaStatusService.evict(fileId);
        boolean has = mediaStatusService.hasAdaptiveStream(fileId);
        return ApiResult.ok(Map.of("fileId", fileId, "adaptive", has, "definition", adaptiveDefinition));
    }

    /**
     * 生成腾讯云 VOD 客户端上传签名。
     * 参考腾讯云 VOD 文档：https://cloud.tencent.com/document/product/266/9221
     * 步骤：
     * 1) 拼接明文串 original（按 QueryString 格式，secretId/currentTimeStamp/expireTime/random 必填）
     * 2) 用 SecretKey 对 original 做 HMAC-SHA1，得到 signatureTmp
     * 3) 把 signatureTmp 和 original.getBytes(UTF-8) 合并，再 Base64 编码得到最终签名
     * 配置了 upload-procedure（任务流名称）时，上传完成后 VOD 会自动执行该任务流（含自适应转码）。
     */
    private String generateClientUploadSignature(long current, long expire) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("secretId", secretId);
        params.put("currentTimeStamp", String.valueOf(current));
        params.put("expireTime", String.valueOf(expire));
        params.put("random", String.valueOf(new java.util.Random().nextInt(1000000000)));
        if (uploadProcedure != null && !uploadProcedure.isBlank()) {
            params.put("procedure", uploadProcedure.trim());
        }

        // 拼接签名原文（key 按字典序）
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) sb.append("&");
            first = false;
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        String original = sb.toString();

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] signatureTmp = mac.doFinal(original.getBytes(StandardCharsets.UTF_8));
            byte[] originalBytes = original.getBytes(StandardCharsets.UTF_8);
            // 合并 signatureTmp + original，再 Base64 编码
            byte[] merged = new byte[signatureTmp.length + originalBytes.length];
            System.arraycopy(signatureTmp, 0, merged, 0, signatureTmp.length);
            System.arraycopy(originalBytes, 0, merged, signatureTmp.length, originalBytes.length);
            return Base64.getEncoder().encodeToString(merged);
        } catch (Exception e) {
            throw new RuntimeException("生成 VOD 上传签名失败", e);
        }
    }
}
