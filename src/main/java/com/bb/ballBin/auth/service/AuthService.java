package com.bb.ballBin.auth.service;

import com.bb.ballBin.common.message.Service.MessageService;
import com.bb.ballBin.security.jwt.BallBinUserDetails;
import com.bb.ballBin.security.jwt.service.JwtBlacklistService;
import com.bb.ballBin.security.jwt.service.RefreshTokenService;
import com.bb.ballBin.security.jwt.util.JwtUtil;
import com.bb.ballBin.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JwtBlacklistService jwtBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final UserService userService;

    /**
     * 로그인 처리
     */
    public ResponseEntity<?> login(String loginId, String loginPassword) {

        try {
            // ✅ 아이디, 비밀번호 인증
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginId, loginPassword)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            BallBinUserDetails userDetails = (BallBinUserDetails) authentication.getPrincipal();
            String userId = userDetails.getUserId();

            Set<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            // ✅ Access Token (1시간)
            String accessToken = jwtUtil.createJwtToken(userId, roles, false);

            // ✅ Refresh Token (3일)
            String refreshToken = jwtUtil.createJwtToken(userId, roles, true);

            // ✅ Refresh Token 을 Redis 에 저장 (권장)
            refreshTokenService.storeRefreshToken(userId, refreshToken);

            // ✅ 응답에 두 개의 토큰 포함
            String jsonResponse = objectMapper.writeValueAsString(Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken
            ));

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ERROR", messageService.getMessage("error.security.password")));
        }
    }

    /**
     * 로그아웃 처리
     */
    public ResponseEntity<?> logout(HttpServletRequest request) {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(messageService.getMessage("error.token.invalid"));
        }

        String token = authorizationHeader.substring(7);
        boolean isOAuth2Token = request.getRequestURI().startsWith("/oauth2/"); // ✅ OAuth2 토큰 여부 체크

        // ✅ 1. 토큰 유효성 검증
        Claims claims = jwtUtil.validateToken(token, false, isOAuth2Token);
        if (claims == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(messageService.getMessage("error.token.invalid"));
        }

        // ✅ 2. 만료 시간 확인
        Date expirationTime = claims.getExpiration();
        if (expirationTime != null) {
            jwtBlacklistService.addToBlacklist(token, expirationTime); // ✅ 블랙리스트 추가
        }

        // ✅ 3. 시큐리티 컨텍스트 초기화
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(messageService.getMessage("success.logout"));
    }


    public ResponseEntity<?> refreshAccessToken(String refreshToken) {
        try {
            boolean isOAuth2Token = refreshToken.startsWith("oauth2-"); // ✅ OAuth2 여부 판별 로직 추가

            // ✅ 1. Refresh Token 검증
            Claims claims = jwtUtil.validateToken(refreshToken, true, isOAuth2Token);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("ERROR", "Invalid Refresh Token"));
            }

            String userId = claims.getSubject();
            String providerId = claims.getSubject();
            String email = claims.get("email", String.class);  // ✅ OAuth2 사용자의 이메일
            String loginId = claims.get("loginId", String.class);  // ✅ OAuth2 사용자의 로그인 ID

            // ✅ 2. Redis 에서 저장된 Refresh Token 확인
            String storedRefreshToken = refreshTokenService.getRefreshToken(userId);
            if (!refreshToken.equals(storedRefreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("ERROR", "Invalid Refresh Token"));
            }

            // ✅ 3. 새로운 Access Token 발급
            Set<String> roles = userService.getUserRoles(userId);

            String newAccessToken = isOAuth2Token ?

                    jwtUtil.createJwtTokenForOAuth2(providerId, email, loginId, roles, false) :
                    jwtUtil.createJwtToken(userId, roles, false);

            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));

        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ERROR", "Refresh Token Expired"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ERROR", "Invalid Refresh Token"));
        }
    }

    public ResponseEntity<?> getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // ✅ 인증되지 않은 사용자 예외 처리
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ERROR", "Unauthorized"));
        }

        // ✅ 현재 로그인한 사용자 정보 가져오기
        BallBinUserDetails userDetails = (BallBinUserDetails) authentication.getPrincipal();

        // ✅ JSON 응답 생성
        Map<String, Object> response = Map.of(
                "userId", userDetails.getUserId(),
                "username", userDetails.getUsername(),
                "email", userDetails.getEmail(),
                "loginId", userDetails.getLoginId()
        );

        return ResponseEntity.ok(response);
    }
}
