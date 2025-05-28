package com.bb.ballBin.common.util;

import com.bb.ballBin.security.jwt.BallBinUserDetails;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.mapper.UserMapper;
import com.bb.ballBin.user.model.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserMapper userMapper;

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

        User user = userDetails.getUser();
        UserResponseDto response = userMapper.toDto(user);

        return ResponseEntity.ok(response);
    }
}
