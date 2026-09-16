package com.ielts.courses.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 大模型服务平台 TokenHub 极简客户端（OpenAI 兼容协议，流式）。
 * hy4-preview 思考链较长，非流式易空闲超时，故统一使用 SSE 流式调用。
 */
@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    @Value("${app.llm.base-url:https://tokenhub.tencentmaas.com}")
    private String baseUrl;

    @Value("${app.llm.api-key:}")
    private String apiKey;

    @Value("${app.llm.model:hy4-preview}")
    private String model;

    @Value("${app.llm.timeout-seconds:300}")
    private int timeoutSeconds;

    private final ObjectMapper mapper = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** 要求模型以 JSON 对象输出的对话补全。 */
    public String chatJson(List<Map<String, String>> messages) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", true);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("stream_options", Map.of("include_usage", true));

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.replaceAll("/+$", "") + "/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("构造 LLM 请求失败", e);
        }

        HttpResponse<InputStream> resp;
        try {
            resp = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            throw new RuntimeException("调用 LLM 失败: " + e.getMessage(), e);
        }

        if (resp.statusCode() != 200) {
            String err;
            try (InputStream is = resp.body()) {
                err = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                err = "";
            }
            throw new RuntimeException("LLM 返回 HTTP " + resp.statusCode() + ": "
                    + err.substring(0, Math.min(300, err.length())));
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String payload = line.substring(5).trim();
                if (payload.isEmpty() || "[DONE]".equals(payload)) continue;
                JsonNode chunk;
                try {
                    chunk = mapper.readTree(payload);
                } catch (Exception e) {
                    log.debug("忽略无法解析的 SSE 片段: {}", payload.substring(0, Math.min(100, payload.length())));
                    continue;
                }
                JsonNode choices = chunk.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    String piece = choices.get(0).path("delta").path("content").asText("");
                    content.append(piece);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("读取 LLM 流式响应失败: " + e.getMessage(), e);
        }
        return content.toString();
    }

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
