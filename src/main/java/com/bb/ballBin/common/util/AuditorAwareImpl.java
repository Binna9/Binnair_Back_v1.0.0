package com.bb.ballBin.common.util;

import com.bb.ballBin.security.jwt.BallBinUserDetails;
import com.bb.ballBin.user.entity.User;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<User> {

    @Override
    public Optional<User> getCurrentAuditor() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof BallBinUserDetails) {
            User user = ((BallBinUserDetails) principal).getUser();
            return Optional.of(user);
        }

        return Optional.empty();
    }
}
