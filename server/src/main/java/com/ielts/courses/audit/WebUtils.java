package com.ielts.courses.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 审计相关工具：取当前请求、客户端 IP/UA、字段截断 */
final class WebUtils {

    private WebUtils() {
    }

    static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    /** 取真实客户端 IP（nginx 反代后优先 X-Forwarded-For 首个） */
    static String clientIp(HttpServletRequest req) {
        if (req == null) return null;
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return truncate(comma > 0 ? xff.substring(0, comma).trim() : xff.trim(), 64);
        }
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return truncate(real.trim(), 64);
        }
        return truncate(req.getRemoteAddr(), 64);
    }

    static String userAgent(HttpServletRequest req) {
        return req == null ? null : truncate(req.getHeader("User-Agent"), 255);
    }

    static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
