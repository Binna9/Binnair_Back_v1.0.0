package com.bb.ballBin.auth.service;

import com.bb.ballBin.common.message.Service.MessageService;
import com.bb.ballBin.security.jwt.BallBinUserDetails;
import com.bb.ballBin.security.jwt.service.JwtBlacklistService;
import com.bb.ballBin.security.jwt.service.RefreshTokenService;
import com.bb.ballBin.security.jwt.util.JwtUtil;
import com.bb.ballBin.user.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final UserService userService;

    /**
     * 로그인 처리
     */
    public ResponseEntity<?> login(String loginId, String loginPassword, HttpServletResponse response) {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        try {
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

            // ✅ Refresh Token 을 Redis 에 저장
            refreshTokenService.storeRefreshToken(userId, refreshToken);

            // ✅ Refresh Token 을 HttpOnly, Secure 쿠키로 설정
            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")   // CSRF 보호
                    .path("/")            // 모든 경로에서 접근 가능
                    .maxAge(3 * 24 * 60 * 60) // 3일 (초 단위)
                    .build();

            response.addHeader("Set-Cookie", refreshTokenCookie.toString());

            // ✅ JSON 응답에 accessToken 만 포함
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("accessToken", accessToken));

        } catch (BadCredentialsException e) {
            logger.warn("로그인 실패 - 아이디 또는 비밀번호 불일치 (ID: {})", loginId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ERROR", messageService.getMessage("error.security.password")));

        } catch (DisabledException e) {
            logger.warn("로그인 실패 - 계정 비활성화됨 (ID: {})", loginId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ERROR", messageService.getMessage("error.security.user.lock")));

        } catch (LockedException e) {
            logger.warn("로그인 실패 - 계정 잠김 (ID: {})", loginId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ERROR", messageService.getMessage("error.security.login.lock")));

        } catch (Exception e) {
            logger.error("로그인 실패 - 예기치 않은 오류 발생 (ID: {})", loginId, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ERROR", messageService.getMessage("error.security.password")));
        }
    }

    /**
     * 로그아웃 처리
     */
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {

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

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false) // 로컬 개발 시 false, 프로덕션에서는 true
                .sameSite("Lax")
                .path("/")
                .maxAge(0) // ✅ 쿠키 즉시 만료
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.ok(messageService.getMessage("success.logout"));
    }

    public ResponseEntity<?> refreshAccessToken(String refreshToken) {

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ERROR", "No Refresh Token provided"));
        }

        try {
            // ✅ 1. Refresh Token 검증
            Claims claims = jwtUtil.validateToken(refreshToken, true, false);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("ERROR", "Invalid Refresh Token"));
            }

            String userId = claims.getSubject();

            // ✅ 2. Redis 에서 저장된 Refresh Token 확인
            String storedRefreshToken = refreshTokenService.getRefreshToken(userId);

            if (!refreshToken.equals(storedRefreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("ERROR", "Invalid Refresh Token"));
            }

            // ✅ 3. 새로운 Access Token 발급
            Set<String> roles = userService.getUserRoles(userId);

            String newAccessToken = jwtUtil.createJwtToken(userId, roles, false);

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
