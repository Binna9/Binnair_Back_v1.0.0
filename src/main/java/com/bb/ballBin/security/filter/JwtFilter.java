package com.bb.ballBin.security.filter;

import com.bb.ballBin.security.jwt.BallBinUserDetails;
import com.bb.ballBin.security.jwt.BallBinUserDetailsService;
import com.bb.ballBin.security.jwt.service.JwtBlacklistService;
import com.bb.ballBin.security.jwt.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final BallBinUserDetailsService ballBinUserDetailsService;
    private final JwtBlacklistService jwtBlacklistService;

    public JwtFilter(JwtUtil jwtUtil, BallBinUserDetailsService ballBinUserDetailsService, JwtBlacklistService jwtBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.ballBinUserDetailsService = ballBinUserDetailsService;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authorization = req.getHeader("Authorization");
        System.out.println("📌 [JwtFilter] 요청 URL: " + req.getRequestURI());
        System.out.println("📌 [JwtFilter] Authorization Header: " + authorization);

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            System.out.println("❌ [JwtFilter] 유효한 Authorization 헤더 없음");
            filterChain.doFilter(req, res);
            return;
        }

        String token = authorization.substring(7);

        if (jwtBlacklistService.isBlacklisted(token)) {
            System.out.println("❌ [JwtFilter] 블랙리스트된 토큰");
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (jwtUtil.isExpired(token)) {
            System.out.println("❌ [JwtFilter] 토큰 만료");
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String userId = jwtUtil.getUserIdFromToken(token);
        System.out.println("✅ [JwtFilter] Extracted UserId: " + userId);

        if (userId != null) {
            BallBinUserDetails ballBinUserDetails = (BallBinUserDetails) ballBinUserDetailsService.loadUserById(userId);

            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    ballBinUserDetails,
                    null,
                    ballBinUserDetails.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);
            System.out.println("✅ [JwtFilter] 인증 성공 - SecurityContext에 저장 완료");
        } else {
            System.out.println("❌ [JwtFilter] userId를 찾을 수 없음");
        }

        filterChain.doFilter(req, res);
    }
}
