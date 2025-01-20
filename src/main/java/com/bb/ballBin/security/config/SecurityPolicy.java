package com.bb.ballBin.security.config;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SecurityPolicy {

    public List<String> getPermittedUrls() {
        return List.of(
                "/",
                "/login",
                "/register",
                "/roles",
                "/menus"
        );
    }
}
