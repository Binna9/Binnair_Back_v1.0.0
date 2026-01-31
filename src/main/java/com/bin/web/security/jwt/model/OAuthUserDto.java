package com.bin.web.security.jwt.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class OAuthUserDto {

    private String providerId;  // ✅ Google 에서 제공하는 `sub` (고유 ID)
    private String email;       // ✅ 사용자 이메일
    private String userName;     // ✅ 사용자 로그인 ID (Google 의 `name` 사용 가능)
    private String profileImageUrl; // ✅ 프로필 이미지 URL
    private Set<String> roles;  // ✅ 사용자 역할 (권한)

}
