package com.bb.ballBin.auth.service;

import com.bb.ballBin.common.exception.NotFoundException;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthHelper {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increaseFailedAttempts(String loginId) {
        try {
            User user = userRepository.findByLoginId(loginId)
                    .orElseThrow(() -> new NotFoundException("error.user.notfound"));

            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            userRepository.save(user);

        } catch (Exception e) {
            System.err.println("FAIL" + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetFailedAttempts(String loginId) {
        try {
            User user = userRepository.findByLoginId(loginId)
                    .orElseThrow(() -> new NotFoundException("error.user.notfound"));

            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        } catch (Exception e) {
            System.err.println("FAIL" + e.getMessage());
        }
    }
}
