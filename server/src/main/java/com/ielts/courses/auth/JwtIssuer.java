package com.ielts.courses.auth;

import com.ielts.courses.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtIssuer {

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtIssuer(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.jwt.secret 必须不少于 32 个字符");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(long userId, String sid, List<String> roles, boolean phoneBound) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + jwtProperties.getTtlSeconds() * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("sid", sid)
                .claim("roles", roles)
                .claim("phoneBound", phoneBound)
                .issuedAt(now)
                .expiration(expire)
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            return null;
        }
    }

    public Long extractUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return null;
    }

    public String extractSid(Claims claims) {
        return claims.get("sid", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        return List.of();
    }

    public boolean extractPhoneBound(Claims claims) {
        Object bound = claims.get("phoneBound");
        return Boolean.TRUE.equals(bound);
    }
}
