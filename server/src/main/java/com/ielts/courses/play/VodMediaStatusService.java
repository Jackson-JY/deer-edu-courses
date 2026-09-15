package com.ielts.courses.play;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * VOD 视频自适应码流转码状态查询，带 Redis 缓存。
 * 播放凭证接口每次打开播放页都会调用，不能每次都请求 VOD API。
 */
@Service
public class VodMediaStatusService {

    private static final String KEY_PREFIX = "vod:adaptive:";

    /** 已有自适应输出：状态不可逆，缓存 6 小时 */
    private static final Duration TTL_YES = Duration.ofHours(6);
    /** 暂无输出（视频可能正在转码）：短缓存 2 分钟，转码完成后很快自动切换 */
    private static final Duration TTL_NO = Duration.ofMinutes(2);
    /** 查询异常：极短缓存防雪崩，同时不阻断播放 */
    private static final Duration TTL_ERROR = Duration.ofSeconds(60);

    private final VodOpenApiClient client;
    private final StringRedisTemplate redis;

    @Value("${app.tencent.vod.adaptive-definition:10}")
    private int adaptiveDefinition;

    public VodMediaStatusService(VodOpenApiClient client, StringRedisTemplate redis) {
        this.client = client;
        this.redis = redis;
    }

    /**
     * 该视频是否已有可播放的自适应码流（HLS）输出。
     * 任何异常都安全降级为 false（继续播放原始文件）。
     */
    public boolean hasAdaptiveStream(String fileId) {
        if (fileId == null || fileId.isBlank() || !client.configured()) {
            return false;
        }
        String key = KEY_PREFIX + fileId;
        String cached = redis.opsForValue().get(key);
        if ("1".equals(cached)) {
            return true;
        }
        if ("0".equals(cached)) {
            return false;
        }

        boolean has;
        try {
            has = client.hasAdaptiveOutput(fileId, adaptiveDefinition);
        } catch (Exception e) {
            redis.opsForValue().set(key, "0", TTL_ERROR);
            return false;
        }
        redis.opsForValue().set(key, has ? "1" : "0", has ? TTL_YES : TTL_NO);
        return has;
    }

    /** 手动触发转码后清除缓存，使下次签发立即生效 */
    public void evict(String fileId) {
        if (fileId != null && !fileId.isBlank()) {
            redis.delete(KEY_PREFIX + fileId);
        }
    }
}
