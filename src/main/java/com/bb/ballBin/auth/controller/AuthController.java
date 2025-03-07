package com.bb.ballBin.auth.controller;

import com.bb.ballBin.auth.service.AuthService;
import com.bb.ballBin.auth.service.OAuth2UserService;
import com.bb.ballBin.security.jwt.model.JwtResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuth2UserService oAuth2UserService;

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest, HttpServletResponse response) {

        String loginId = loginRequest.get("loginId");
        String loginPassword = loginRequest.get("loginPassword");

        return authService.login(loginId, loginPassword, response);
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {

        return authService.logout(request, response);
    }

    @PostMapping("/google/login")
    public ResponseEntity<JwtResponse> googleLogin(@RequestBody Map<String, String> request) {

        String code = request.get("code");
        // ✅ Google OAuth 로그인 및 회원가입 처리 후 JWT 발급
        String accessToken = oAuth2UserService.handleGoogleLogin(code);

        return ResponseEntity.ok(new JwtResponse(accessToken));
    }

//    @PostMapping("/kakao/login")
//    public ResponseEntity<JwtResponse> kakaoLogin(@RequestBody Map<String, String> request) {
//
//        String code = request.get("code");
//         ✅ Google OAuth 로그인 및 회원가입 처리 후 JWT 발급
//        String accessToken = oAuth2UserService.handleGoogleLogin(code);
//
//        return ResponseEntity.ok(new JwtResponse(accessToken));
//    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        return authService.refreshAccessToken(refreshToken);
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUser() {
        return authService.getCurrentUser();
    }
}