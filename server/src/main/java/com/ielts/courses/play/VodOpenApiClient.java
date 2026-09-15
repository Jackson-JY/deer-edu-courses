package com.ielts.courses.play;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 腾讯云 VOD 开放 API 极简客户端（TC3-HMAC-SHA256 签名，不引入官方 SDK）。
 * 仅封装本项目用到的两个接口：
 * - DescribeMediaInfos：查询媒体是否已生成自适应码流（HLS）输出
 * - ProcessMedia：对存量视频发起转自适应码流任务
 */
@Component
public class VodOpenApiClient {

    private static final String HOST = "vod.tencentcloudapi.com";
    private static final String ENDPOINT = "https://" + HOST;
    private static final String SERVICE = "vod";
    private static final String VERSION = "2018-07-17";

    @Value("${app.tencent.vod.secret-id:}")
    private String secretId;

    @Value("${app.tencent.vod.secret-key:}")
    private String secretKey;

    @Value("${app.tencent.vod.sub-app-id:0}")
    private long subAppId;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public boolean configured() {
        return secretId != null && !secretId.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }

    /**
     * 查询媒体是否已生成指定模板的自适应码流输出。
     *
     * @param definition 自适应码流模板 ID，&lt;=0 表示存在任意自适应输出即可
     */
    public boolean hasAdaptiveOutput(String fileId, int definition) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("FileIds", List.of(fileId));
            if (subAppId > 0) {
                body.put("SubAppId", subAppId);
            }
            JsonNode resp = call("DescribeMediaInfos", mapper.writeValueAsString(body));
            JsonNode set = resp.path("MediaInfoSet");
            if (!set.isArray() || set.isEmpty()) {
                return false;
            }
            // 响应路径：MediaInfoSet[0].AdaptiveDynamicStreamingInfo.AdaptiveDynamicStreamingSet[]
            JsonNode ads = set.get(0).path("AdaptiveDynamicStreamingInfo")
                    .path("AdaptiveDynamicStreamingSet");
            if (!ads.isArray()) {
                return false;
            }
            for (JsonNode ad : ads) {
                int def = ad.path("Definition").asInt(0);
                if (definition <= 0 || def == definition) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            // 查询失败不阻断播放：抛出由调用方按"未转码"降级（Original 仍可播）
            throw new RuntimeException("查询 VOD 转码状态失败: " + e.getMessage(), e);
        }
    }

    /** 发起转自适应码流任务，返回 VOD 任务 ID */
    public String processAdaptive(String fileId, int definition) throws Exception {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("Definition", definition);
        Map<String, Object> mediaProcessTask = new LinkedHashMap<>();
        mediaProcessTask.put("AdaptiveDynamicStreamingTaskSet", List.of(task));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("FileId", fileId);
        body.put("MediaProcessTask", mediaProcessTask);
        if (subAppId > 0) {
            body.put("SubAppId", subAppId);
        }
        JsonNode resp = call("ProcessMedia", mapper.writeValueAsString(body));
        return resp.path("TaskId").asText(null);
    }

    private JsonNode call(String action, String jsonBody) throws Exception {
        long ts = Instant.now().getEpochSecond();
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneOffset.UTC).format(Instant.ofEpochSecond(ts));

        // 1. 拼接规范请求串
        String canonicalHeaders = "content-type:application/json; charset=utf-8\n"
                + "host:" + HOST + "\n"
                + "x-tc-action:" + action.toLowerCase() + "\n";
        String signedHeaders = "content-type;host;x-tc-action";
        String canonicalRequest = "POST\n/\n\n" + canonicalHeaders + "\n"
                + signedHeaders + "\n" + sha256Hex(jsonBody);

        // 2. 拼接待签名串
        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String stringToSign = "TC3-HMAC-SHA256\n" + ts + "\n" + credentialScope + "\n"
                + sha256Hex(canonicalRequest);

        // 3. 逐层计算签名
        byte[] kDate = hmac(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] kService = hmac(kDate, SERVICE);
        byte[] kSigning = hmac(kService, "tc3_request");
        String signature = HexFormat.of().formatHex(hmac(kSigning, stringToSign));

        String authorization = "TC3-HMAC-SHA256 Credential=" + secretId + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("X-TC-Action", action)
                .header("X-TC-Version", VERSION)
                .header("X-TC-Timestamp", String.valueOf(ts))
                .header("Authorization", authorization)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode root = mapper.readTree(response.body());
        JsonNode respNode = root.path("Response");
        JsonNode error = respNode.path("Error");
        if (!error.isMissingNode() && !error.isNull()) {
            throw new RuntimeException(error.path("Code").asText() + " " + error.path("Message").asText());
        }
        return respNode;
    }

    private static String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] hmac(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }
}
