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
                "/auth/login",
                "/auth/logout",
                "/registers"
        );
    }

    /**
     * 인증된 사용자만 접근 가능
     */
    public List<String> getAuthenticatedUrls() {
        return List.of(
                "/roles/**",
                "/menus/**",
                "/boards/**",
                "/bookmarks/**",
                "/users/**",
                "/carts/**",
                "/products/**"
        );
    }

    /**
     * 관리자만 접근 가능
     */
    public List<String> getAdminUrls() {
        return List.of(
                "/admin/**"
        );
    }
}
