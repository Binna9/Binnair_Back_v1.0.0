package com.bin.web.auth.service;

import com.bin.web.common.exception.NotFoundException;
import com.bin.web.user.entity.User;
import com.bin.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthHelper {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int increaseFailedAttemptsAndGet(String loginId) {
        Integer failed = userRepository.incrementFailedAttempts(loginId);
        if (failed == null) {
            throw new NotFoundException("error.user.notfound");
        }
        return failed;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetFailedAttempts(String loginId) {
        try {
            User user = userRepository.findByLoginId(loginId)
                    .orElseThrow(() -> new NotFoundException("error.user.notfound"));

            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("error.runtime", e);
        }
    }
}
