package com.bb.ballBin.auth.service;

import com.bb.ballBin.security.jwt.model.OAuthUserDto;
import com.bb.ballBin.security.jwt.util.JwtUtil;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OAuth2UserService {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ✅ Google OAuth 관련 URL
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    // ✅ OAuth2 설정 (Google Cloud 콘솔에서 설정한 값)
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String CLIENT_ID;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String CLIENT_SECRET;
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String REDIRECT_URI;

    /**
     * ✅ Google OAuth2 로그인 후 사용자 정보 조회 및 회원가입/로그인 처리
     */
    public String handleGoogleLogin(String code) {
        // ✅ 1. Google 서버에서 AccessToken 요청
        String accessToken = getGoogleAccessToken(code);

        // ✅ 2. AccessToken 으로 Google 사용자 정보 가져오기
        OAuthUserDto userDto = fetchGoogleUserProfile(accessToken);

        // ✅ 3. DB 에서 사용자 조회 (providerId 기준)
        Optional<User> existingUser = userService.findByProviderId(userDto.getProviderId());

        User user;
        if (existingUser.isEmpty()) {
            // ✅ 4. 신규 사용자 회원가입
            user = userService.registerOAuthUser(userDto);
        } else {
            // ✅ 5. 기존 사용자 로그인 처리
            user = existingUser.get();
        }

        // ✅ 6. JWT 발급 후 반환
        Set<String> roles = userService.getUserRoles(user.getUserId());

        return jwtUtil.createJwtToken(user.getUserId(), roles, false);
    }

    /**
     * ✅ `code`를 Google 서버에 보내 `accessToken` 요청하는 메서드
     */
    private String getGoogleAccessToken(String code) {

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // ✅ Google 서버에 보낼 요청 파라미터
        String requestBody = "code=" + code +
                "&client_id=" + CLIENT_ID +
                "&client_secret=" + CLIENT_SECRET +
                "&redirect_uri=" + REDIRECT_URI +
                "&grant_type=authorization_code";

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        // ✅ Google 서버로 POST 요청 (AccessToken 요청)
        ResponseEntity<String> response = restTemplate.exchange(GOOGLE_TOKEN_URL, HttpMethod.POST, request, String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("access_token").asText(); // ✅ Google 에서 받은 AccessToken 반환
        } catch (Exception e) {
            throw new RuntimeException("❌ Google Access Token 요청 실패", e);
        }
    }

    /**
     * ✅ `accessToken`을 사용해 Google 사용자 정보를 요청하는 메서드
     */
    private OAuthUserDto fetchGoogleUserProfile(String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // ✅ Google 사용자 정보 요청 (Authorization 헤더에 AccessToken 추가)
        ResponseEntity<String> response = restTemplate.exchange(GOOGLE_USER_INFO_URL, HttpMethod.GET, request, String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            return new OAuthUserDto(
                    jsonNode.get("id").asText(),       // ✅ Google `sub` (providerId)
                    jsonNode.get("email").asText(),    // ✅ 사용자 이메일
                    jsonNode.get("name").asText(),     // ✅ 사용자 이름
                    jsonNode.get("picture").asText(),  // ✅ 프로필 이미지 URL
                    Collections.singleton("ROLE_USER") // ✅ 기본 권한
            );

        } catch (Exception e) {
            throw new RuntimeException("❌ Google 사용자 정보 가져오기 실패", e);
        }
    }
}
