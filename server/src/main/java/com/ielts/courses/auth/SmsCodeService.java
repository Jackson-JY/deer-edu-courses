package com.ielts.courses.auth;

import com.ielts.courses.common.ApiException;
import com.ielts.courses.common.BizCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
public class SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeService.class);
    private static final String CODE_KEY = "sms:code:";
    private static final String SEND_LOCK_KEY = "sms:send:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SEND_LOCK_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;

    public SmsCodeService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String sendCode(String phone) {
        String lockKey = SEND_LOCK_KEY + phone;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", SEND_LOCK_TTL);
        if (Boolean.FALSE.equals(locked)) {
            throw new ApiException(BizCode.BAD_REQUEST, "验证码发送过于频繁，请 60 秒后再试");
        }

        String code = generateCode();
        redisTemplate.opsForValue().set(CODE_KEY + phone, code, CODE_TTL);
        // 开发环境直接打印；生产环境替换为短信 SDK 调用
        log.info("[SMS MOCK] phone={} code={}", phone, code);
        return code;
    }

    public boolean verify(String phone, String code) {
        String key = CODE_KEY + phone;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null || !stored.equals(code)) {
            return false;
        }
        redisTemplate.delete(key);
        return true;
    }

    private String generateCode() {
        Random random = new Random();
        int value = random.nextInt(900000) + 100000;
        return String.valueOf(value);
    }
}
