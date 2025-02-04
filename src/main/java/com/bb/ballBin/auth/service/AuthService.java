package com.bb.ballBin.auth.service;

import com.bb.ballBin.common.message.Service.MessageService;
import com.bb.ballBin.security.jwt.BallBinUserDetails;
import com.bb.ballBin.security.jwt.service.JwtBlacklistService;
import com.bb.ballBin.security.jwt.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JwtBlacklistService jwtBlacklistService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil, JwtBlacklistService jwtBlacklistService, MessageService messageService, ObjectMapper objectMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.jwtBlacklistService = jwtBlacklistService;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    /**
     * 로그인 처리
     */
    public ResponseEntity<?> login(String loginId, String loginPassword) {

        try {
            // ✅ 아이디, 비밀번호 기반으로 Spring Security 인증 시도
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginId, loginPassword)
            );

            // ✅ 인증된 사용자 정보 가져오기
            SecurityContextHolder.getContext().setAuthentication(authentication);
            BallBinUserDetails userDetails = (BallBinUserDetails) authentication.getPrincipal();

            String userId = userDetails.getUserId();

            Set<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            // ✅ JWT 토큰 생성 (10시간 만료)
            String token = jwtUtil.createJwtToken(userId, roles, 60 * 60 * 10L * 1000);

            String jsonResponse = objectMapper.writeValueAsString(Map.of("token", token));

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
        Date expirationTime = jwtUtil.getExpiration(token);

        if (expirationTime != null) {
            jwtBlacklistService.addToBlacklist(token, expirationTime); // ✅ 만료시간 포함해서 블랙리스트 추가
        }

        SecurityContextHolder.clearContext(); // 시큐리티 컨텍스트 초기화

        return ResponseEntity.ok(messageService.getMessage("success.logout"));
    }
}
