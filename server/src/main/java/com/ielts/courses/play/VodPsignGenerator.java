package com.ielts.courses.play;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 腾讯云 VOD 播放器签名（psign）生成器。
 *
 * 签名格式：JWT (JSON Web Token)
 *   base64UrlEncode(Header) + "." + base64UrlEncode(Payload) + "." + base64UrlEncode(Signature)
 *
 * - Header:   {"alg":"HS256","typ":"JWT"}
 * - Payload: 签名参数（appId, fileId, currentTimeStamp, expireTimeStamp, urlAccessInfo, contentInfo）
 * - Signature: base64UrlEncode(HMACSHA256(base64UrlEncode(Header)+"."+base64UrlEncode(Payload), playKey))
 *
 * contentInfo 规范（https://cloud.tencent.com/document/product/266/45554）：
 * - audioVideoType 必选，String 类型，可选值：
 *   - "Original"           上传的原始音视频（无需额外参数）
 *   - "Transcode"          转码输出（需 transcodeDefinition）
 *   - "RawAdaptive"        未加密转自适应码流（需 rawAdaptiveDefinition）
 *   - "ProtectedAdaptive"  加密保护转自适应码流（需 drmAdaptiveInfo）
 */
@Component
public class VodPsignGenerator {

    private static final String HS256 = "HS256";
    private static final String ORIGINAL = "Original";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 生成 psign（JWT 格式）。
     *
     * @param appId     腾讯云 VOD subAppId
     * @param fileId    视频 fileId
     * @param playKey   播放器签名密钥
     * @param ttl       签名有效时长（秒）
     * @param watermark 动态水印文本（可选）
     */
    public String generate(long appId, String fileId, String playKey, long ttl, String watermark) {
        long current = System.currentTimeMillis() / 1000;
        long expire = current + ttl;

        // Header
        Map<String, String> header = new LinkedHashMap<>();
        header.put("alg", HS256);
        header.put("typ", "JWT");
        String headerB64 = b64url(toJson(header));

        // Payload - 签名参数
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appId", appId);
        payload.put("fileId", fileId);
        payload.put("currentTimeStamp", current);
        payload.put("expireTimeStamp", expire);

        Map<String, Object> urlAccessInfo = new LinkedHashMap<>();
        urlAccessInfo.put("t", String.valueOf(expire));
        payload.put("urlAccessInfo", urlAccessInfo);

        // contentInfo - 必选，至少包含 audioVideoType
        // 目前未配置转码模板，使用 Original（原始视频）
        Map<String, Object> contentInfo = new LinkedHashMap<>();
        contentInfo.put("audioVideoType", ORIGINAL);
        if (watermark != null && !watermark.isBlank()) {
            contentInfo.put("watermarkText", watermark);
        }
        payload.put("contentInfo", contentInfo);

        String payloadB64 = b64url(toJson(payload));

        // 签名: base64UrlEncode(HMACSHA256(header.payload))
        String signingInput = headerB64 + "." + payloadB64;
        byte[] signatureBytes = hmacSha256Bytes(playKey, signingInput);
        String signatureB64 = b64url(signatureBytes);

        return signingInput + "." + signatureB64;
    }

    private String hmacSha256Hex(String key, String data) {
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

    private String b64url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private String b64url(String s) {
        return b64url(s.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] hmacSha256Bytes(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("生成签名失败", e);
        }
    }

    private String toJson(Map<String, ?> map) {
        try {
            return MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }
}
