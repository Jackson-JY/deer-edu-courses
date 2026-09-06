package com.ielts.courses.config;

import com.ielts.courses.auth.JwtIssuer;
import com.ielts.courses.auth.LoginUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtIssuer jwtIssuer;
    private final StringRedisTemplate redisTemplate;

    public JwtAuthFilter(JwtIssuer jwtIssuer, StringRedisTemplate redisTemplate) {
        this.jwtIssuer = jwtIssuer;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        Claims claims = jwtIssuer.parse(token);
        if (claims == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = jwtIssuer.extractUserId(claims);
        String sid = jwtIssuer.extractSid(claims);
        if (userId == null || !StringUtils.hasText(sid)) {
            filterChain.doFilter(request, response);
            return;
        }

        String activeSid = redisTemplate.opsForValue().get("login:session:" + userId);
        if (!sid.equals(activeSid)) {
            // 会话已失效：清除上下文后继续，由 Spring Security 授权层决定是否拦截
            // 公开接口（如发送验证码）可正常通过；受保护接口会返回 403，前端据此提示重新登录
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        List<String> roles = jwtIssuer.extractRoles(claims);
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());

        LoginUser loginUser = new LoginUser(userId, sid, roles, jwtIssuer.extractPhoneBound(claims));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
