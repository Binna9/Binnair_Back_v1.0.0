package com.bin.web.auth.service;

import com.bin.web.common.exception.InvalidPasswordException;
import com.bin.web.common.Service.MessageService;
import com.bin.web.common.exception.IsActiveAccountException;
import com.bin.web.security.jwt.service.JwtBlacklistService;
import com.bin.web.security.jwt.service.RefreshTokenService;
import com.bin.web.security.jwt.util.JwtUtil;
import com.bin.web.user.entity.User;
import com.bin.web.user.repository.UserRepository;
import com.bin.web.user.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JwtBlacklistService jwtBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final MessageService messageService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthHelper authHelper;

    /**
     * 로그인 처리
     */
    public ResponseEntity<?> login(String loginId, String loginPassword, HttpServletResponse response) {

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("error.login.error.notfound"));

        if (!user.isActive()) {
            throw new IsActiveAccountException("error.login.assign");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginId, loginPassword)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String userId = user.getUserId();
            Set<String> roles = userService.getUserRoleNames(userId);

            authHelper.resetFailedAttempts(user.getLoginId());

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
            int failed = authHelper.increaseFailedAttemptsAndGet(loginId);
            if (failed >= 5) throw new InvalidPasswordException("error.security.login.lock");
            throw new InvalidPasswordException("error.security.password");
        }
    }

    /**
     * 로그아웃 처리
     */
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
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
        } catch (Exception e){
            throw new RuntimeException("error.runtime", e);
        }
    }

    /**
     * Token Refresh
     */
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
            Set<String> roles = userService.getUserRoleNames(userId);

            String newAccessToken = jwtUtil.createJwtToken(userId, roles, false);

            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));

        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ERROR", "Refresh Token Expired"));
        } catch (Exception e) {
            throw new RuntimeException("error.runtime", e);
        }
    }
}
