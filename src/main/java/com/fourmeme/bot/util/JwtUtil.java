package com.fourmeme.bot.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-hours:24}")
    private long expireHours;

    @Value("${jwt.temp-expire-minutes:10}")
    private long tempExpireMinutes;

    private SecretKey key() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        return Keys.hmacShaKeyFor(bytes);
    }
    /** 生成完整 token（带版本号） */
    public String generateToken(Long userId, String username, Integer tokenVersion) {
        return build(userId, username, tokenVersion, expireHours * 60 * 60 * 1000L, "full");
    }

    /** 生成临时 token（不需要版本号） */
    public String generateTempToken(Long userId, String username) {
        return build(userId, username, null, tempExpireMinutes * 60 * 1000L, "temp");
    }

    private String build(Long userId, String username, Integer tokenVersion,
                         long expireMs, String type) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMs);
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("username", username);
        claims.put("type", type);
        if (tokenVersion != null) {
            claims.put("ver", tokenVersion);
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 从 token 取版本号 */
    public Integer getTokenVersion(String token) {
        Claims c = parse(token);
        if (c == null) return null;
        Object v = c.get("ver");
        return v != null ? Integer.valueOf(v.toString()) : null;
    }

    public Claims parse(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    public Long getUserId(String token) {
        Claims c = parse(token);
        if (c == null) return null;
        Object uid = c.get("uid");
        return uid != null ? Long.valueOf(uid.toString()) : null;
    }

    public String getUsername(String token) {
        Claims c = parse(token);
        return c != null ? (String) c.get("username") : null;
    }

    public String getType(String token) {
        Claims c = parse(token);
        return c != null ? (String) c.get("type") : null;
    }

    public boolean isFullToken(String token) {
        return "full".equals(getType(token));
    }

    public boolean isTempToken(String token) {
        return "temp".equals(getType(token));
    }

    /** 从 Authorization 头里提取 token */
    public String extractFromHeader(String header) {
        if (header == null) return null;
        if (header.startsWith("Bearer ")) return header.substring(7);
        return header;
    }
}