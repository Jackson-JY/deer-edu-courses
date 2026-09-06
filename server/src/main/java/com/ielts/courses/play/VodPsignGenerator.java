package com.ielts.courses.play;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class VodPsignGenerator {

    /**
     * 生成腾讯云 VOD 播放器签名（psign）。
     *
     * @param appId     腾讯云 VOD subAppId
     * @param fileId    视频 fileId
     * @param secret    播放器签名密钥（app.tencent.vod.play-key）
     * @param ttl       签名有效时长（秒）
     * @param watermark 动态水印文本
     */
    public String generate(long appId, String fileId, String secret, long ttl, String watermark) {
        long current = System.currentTimeMillis() / 1000;
        long expire = current + ttl;

        Map<String, Object> urlAccessInfo = new HashMap<>();
        urlAccessInfo.put("t", String.valueOf(expire));

        Map<String, Object> contentInfo = new HashMap<>();
        if (watermark != null && !watermark.isEmpty()) {
            contentInfo.put("watermarkText", watermark);
        }

        Map<String, Object> psign = new HashMap<>();
        psign.put("appId", appId);
        psign.put("fileId", fileId);
        psign.put("currentTimeStamp", current);
        psign.put("expireTimeStamp", expire);
        psign.put("urlAccessInfo", urlAccessInfo);
        if (!contentInfo.isEmpty()) {
            psign.put("contentInfo", contentInfo);
        }

        String json = toJson(psign);
        String signature = hmacSha256(secret, json);
        String payload = json + signature;
        return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("生成 psign 失败", e);
        }
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof Map) {
                sb.append(toJson((Map<String, Object>) value));
            } else if (value instanceof Number) {
                sb.append(value);
            } else {
                sb.append("\"").append(value).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
