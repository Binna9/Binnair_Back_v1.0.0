package com.bb.ballBin.security.jwt.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
            System.out.println("📌 Token Expiration Time: " + expiration);
            System.out.println("📌 Current Time: " + new Date());

            return expiration.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("❌ Token validation failed: " + e.getMessage());
            e.printStackTrace();
            return true; // 🚨 예외 발생 시 만료된 것으로 간주
        }
    }

    public String createJwtToken(String userId, Set<String> roles, Long expiredMs) {

        String token = Jwts.builder()
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        // 🔍 생성된 JWT 의 Payload 를 확인하는 코드 추가
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            System.out.println("✅ JWT Payload: " + claims);
        } catch (Exception e) {
            System.out.println("❌ JWT Payload 확인 실패!");
            e.printStackTrace();
        }

        System.out.println("✅ Created JWT: " + token);
        return token;
    }
}
