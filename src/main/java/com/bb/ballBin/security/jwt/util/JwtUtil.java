package com.bb.ballBin.security.jwt.util;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(@Value("${spring.jwt.secret}")String secret) {
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    public String getUserIdFromToken(String token) {
        try {
            System.out.println("📌 Parsing Token: " + token);
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            System.out.println("✅ Extracted Claims: " + claims);

            // ✅ 기존 코드에서 .get("userId", String.class) 대신 Object 로 받아 변환
            Object userIdObject = claims.get("userId");
            String userId = userIdObject != null ? String.valueOf(userIdObject) : null;

            System.out.println("✅ Extracted userId: " + userId);
            return userId;
        } catch (JwtException | IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Date getExpiration(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public String getLoginIdFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("loginId", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public Set<String> getRolesFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            List<String> roles = claims.get("roles", List.class);
            return roles != null ? Set.copyOf(roles) : Set.of();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();

            return expiration.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("❌ Token validation failed: " + e.getMessage());
            e.printStackTrace();
            return true; // 🚨 예외 발생 시 만료된 것으로 간주
        }
    }

    public String createJwtToken(String userId, Set<String> roles, Long expiredMs, boolean isRefreshToken) {

        long now = System.currentTimeMillis(); // ✅ 현재 시간 설정

        JwtBuilder jwtBuilder = Jwts.builder()
                .claim("userId", userId)
                .issuedAt(new Date(now)) // ✅ 현재 시간 기준으로 issuedAt 설정
                .expiration(new Date(now + expiredMs)) // ✅ 유효시간 계산
                .signWith(secretKey, SignatureAlgorithm.HS256);

        if (!isRefreshToken) { // ✅ Access Token 일 때만 roles 포함
            jwtBuilder.claim("roles", roles);
        }

        return jwtBuilder.compact();
    }

    /** ✅ Refresh Token 및 Access Token 검증 로직 추가 */
    public Claims validateToken(String token, boolean isRefreshToken) {

        try {
            Claims claims = parseToken(token);

            if (isRefreshToken && claims.get("roles") != null) {
                System.out.println("❌ [JwtUtil] 잘못된 Refresh Token");
                return null;
            }

            return claims; // ✅ 정상적인 토큰이면 Claims 반환
        } catch (JwtException e) {
            System.out.println("❌ [JwtUtil] 토큰 검증 실패: " + e.getMessage());
            return null;
        }
    }
}
