package com.ielts.courses.admin;

import com.ielts.courses.common.ApiResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
     * 生成腾讯云 VOD 客户端上传签名。
     * 参考腾讯云 VOD 文档：https://cloud.tencent.com/document/product/266/9221
     * 步骤：
     * 1) 拼接明文串 original（按 QueryString 格式，secretId/currentTimeStamp/expireTime/random 必填）
     * 2) 用 SecretKey 对 original 做 HMAC-SHA1，得到 signatureTmp
     * 3) 把 signatureTmp 和 original.getBytes(UTF-8) 合并，再 Base64 编码得到最终签名
     */
    private String generateClientUploadSignature(long current, long expire) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("secretId", secretId);
        params.put("currentTimeStamp", String.valueOf(current));
        params.put("expireTime", String.valueOf(expire));
        params.put("random", String.valueOf(new java.util.Random().nextInt(1000000000)));

        // 拼接签名原文
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
