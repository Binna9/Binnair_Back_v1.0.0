package com.bb.ballBin.common.util;

import com.bb.ballBin.security.jwt.util.JwtUtil;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<User> {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final HttpServletRequest request;

    public AuditorAwareImpl(JwtUtil jwtUtil, UserRepository userRepository, HttpServletRequest request) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.request = request;
    }

    @Override
    public Optional<User> getCurrentAuditor() {
        // 1️⃣ 요청 헤더에서 JWT 토큰 가져오기
        String token = getTokenFromRequest();
        if (!StringUtils.hasText(token) || jwtUtil.isExpired(token)) {
            return Optional.empty();
        }

        // 2️⃣ JWT에서 userId 추출
        String userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Optional.empty();
        }

        // 3️⃣ userId로 User 엔티티 조회 후 반환
        return userRepository.findById(userId);
    }

    private String getTokenFromRequest() {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
