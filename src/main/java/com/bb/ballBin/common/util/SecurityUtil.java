package com.bb.ballBin.common.util;

import com.bb.ballBin.security.jwt.BallBinUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SecurityUtil {

    /**
     * 사용자 ID
     */
    public static String getCurrentUserId() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            BallBinUserDetails ballBinUserDetails = (BallBinUserDetails) authentication.getPrincipal();
            return ballBinUserDetails.getUserId();
        }

        throw new RuntimeException("error.login.notfound");
    }

    /**
     * 로그인 사용자 정보
     */
    public ResponseEntity<?> getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ERROR", "Unauthorized"));
        }

        BallBinUserDetails userDetails = (BallBinUserDetails) authentication.getPrincipal();

        Map<String, Object> response = Map.of(
                "userId", userDetails.getUserId(),
                "username", userDetails.getUsername(),
                "email", userDetails.getEmail(),
                "loginId", userDetails.getLoginId()
        );

        return ResponseEntity.ok(response);
    }
}
