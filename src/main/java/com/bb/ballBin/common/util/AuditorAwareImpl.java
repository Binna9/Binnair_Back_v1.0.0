package com.bb.ballBin.common.util;

import com.bb.ballBin.security.jwt.BallBinUserDetails;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<User> {

    private final UserRepository userRepository;
    @Override
    public Optional<User> getCurrentAuditor() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return userRepository.findByLoginId("system");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof BallBinUserDetails) {
            return Optional.of(((BallBinUserDetails) principal).getUser());
        }

        return userRepository.findByLoginId("system");
    }
}
