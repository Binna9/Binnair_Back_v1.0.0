package com.bb.ballBin.security.config;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SecurityPolicy {

    /**
     * 인증 없이 사용 가능
     */
    public List<String> getPermittedUrls() {
        return List.of(
                "/auth/**",
                "/registers",
                "/products/**",
                "/swagger-ui/**",
                "/api-docs/**",
                "/v3/api-docs/**",
                "/websocket"
        );
    }

    /**
     * 인증된 사용자만 접근 가능
     */
    public List<String> getAuthenticatedUrls() {
        return List.of(
                "/boards/**",
                "/roles/**",
                "/permissions/**",
                "/menus/**",
                "/bookmarks/**",
                "/users/**",
                "/carts/**",
                "/addresses/**",
                "/comments/**",
                "/likes/**",
                "/files/**",
                "/chats/**"
        );
    }
}
