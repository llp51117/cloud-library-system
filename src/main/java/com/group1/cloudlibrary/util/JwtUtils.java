package com.group1.cloudlibrary.util;

import com.group1.cloudlibrary.config.CloudLibraryProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类（JJWT 0.12.x）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final CloudLibraryProperties properties;

    private SecretKey getKey() {
        String secret = properties.getJwt().getSecret();
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成令牌
     *
     * @param userId 用户ID
     * @param email  邮箱
     * @param role   角色（0普通用户 / 1管理员）
     */
    public String generateToken(Long userId, String email, Integer role) {
        long expireMillis = properties.getJwt().getExpireHours() * 60 * 60 * 1000;
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析令牌，失败返回 null
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }
}
