package com.bb.ballBin.common.util;

import com.bb.ballBin.security.jwt.BallBinUserDetails;
import com.bb.ballBin.user.entity.AuthProvider;
import com.bb.ballBin.user.entity.User;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<User> {

    private static final User SYSTEM_USER;

    static {
        SYSTEM_USER = new User();
        SYSTEM_USER.setUserId("00000000-0000-0000-0000-000000000000");
        SYSTEM_USER.setLoginId("system");
        SYSTEM_USER.setUserName("system");
        SYSTEM_USER.setEmail("system@system.com");
        SYSTEM_USER.setProvider(AuthProvider.SYSTEM);
        SYSTEM_USER.setProviderId("system");
    }

    @Override
    public Optional<User> getCurrentAuditor() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
                return Optional.of(SYSTEM_USER);
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof BallBinUserDetails userDetails) {
                return Optional.of(userDetails.getUser());
            }

        } catch (Exception e) {
            System.err.println("[AuditorAwareImpl] 예외 발생: " + e.getMessage());
        }
        return Optional.of(SYSTEM_USER);
    }
}


