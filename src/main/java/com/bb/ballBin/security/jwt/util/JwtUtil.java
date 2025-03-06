package com.bb.ballBin.security.jwt.util;

import com.bb.ballBin.security.jwt.config.JwtProperties;
import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final SecretKey oauth2SecretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtUtil(JwtProperties jwtProperties) {
        this.secretKey = new SecretKeySpec(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm());
        this.oauth2SecretKey = new SecretKeySpec(jwtProperties.getOauth2Secret().getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm());

        this.accessTokenExpiration = jwtProperties.getAccessTokenExpiration();
        this.refreshTokenExpiration = jwtProperties.getRefreshTokenExpiration();
    }

    /** ✅ 일반 로그인용 JWT 생성 */
    public String createJwtToken(String userId, Set<String> roles, boolean isRefreshToken) {
        Map<String, Object> claims = isRefreshToken
                ? Map.of() // ✅ Refresh Token 에는 claims 없음
                : Map.of("roles", roles); // ✅ Access Token 에는 roles 추가

        return generateToken(userId, claims, isRefreshToken, false);
    }

    /** ✅ OAuth2 로그인용 JWT 생성 */
    public String createJwtTokenForOAuth2(String providerId, String email, String loginId, Set<String> roles, boolean isRefreshToken) {
        Map<String, Object> claims = Map.of(
                "roles", roles,
                "email", email,
                "loginId", loginId
        );

        return generateToken(providerId, claims, isRefreshToken, true);
    }

    /** ✅ JWT 토큰 생성 (공통) */
    private String generateToken(String subject, Map<String, Object> claims, boolean isRefreshToken, boolean isOAuth2) {

        long expirationTime = isRefreshToken ? refreshTokenExpiration : accessTokenExpiration;

        SecretKey key = isOAuth2 ? oauth2SecretKey : secretKey;

        JwtBuilder jwtBuilder = Jwts.builder()
                .setSubject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key, SignatureAlgorithm.HS256);

        if (!isRefreshToken) { // ✅ Refresh Token 이 면 claims 추가 안 함
            claims.forEach(jwtBuilder::claim);
        }

        return jwtBuilder.compact();
    }

    /** ✅ 토큰 검증 및 파싱 */
    public Claims parseToken(String token, boolean isOAuth2) {

        try {
            SecretKey key = isOAuth2 ? oauth2SecretKey : secretKey;
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            return null;
        }
    }

    /** ✅ 사용자 ID (userId 또는 providerId) 추출 */
    public String getUserIdFromToken(String token, boolean isOAuth2) {
        Claims claims = parseToken(token, isOAuth2);
        return claims != null ? claims.getSubject() : null;
    }

    /** ✅ JWT 토큰 만료 여부 확인 */
    public boolean isExpired(String token, boolean isOAuth2) {
        Claims claims = parseToken(token, isOAuth2);
        return claims == null || claims.getExpiration().before(new Date());
    }

    /** ✅ JWT 토큰에서 권한 정보 추출 */
    public Set<String> getRolesFromToken(String token, boolean isOAuth2) {
        Claims claims = parseToken(token, isOAuth2);
        List<String> roles = claims != null ? claims.get("roles", List.class) : null;
        return roles != null ? Set.copyOf(roles) : Set.of();
    }

    public Claims validateToken(String token, boolean isRefreshToken, boolean isOAuth2) {
        try {
            Claims claims = parseToken(token, isOAuth2);
            if (isRefreshToken && claims.get("roles") != null) {
                return null; // ✅ Refresh Token 에 roles 가 포함되어 있으면 잘못된 토큰
            }
            return claims; // ✅ 정상적인 토큰이면 Claims 반환
        } catch (JwtException e) {
            return null;
        }
    }
}
