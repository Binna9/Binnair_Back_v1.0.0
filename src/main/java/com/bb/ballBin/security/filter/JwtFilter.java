package com.bb.ballBin.security.filter;

import com.bb.ballBin.security.jwt.BallBinUserDetails;
import com.bb.ballBin.security.jwt.BallBinUserDetailsService;
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

    public JwtFilter(JwtUtil jwtUtil, BallBinUserDetailsService ballBinUserDetailsService) {
        this.jwtUtil = jwtUtil;
        this.ballBinUserDetailsService = ballBinUserDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authorization= req.getHeader("Authorization");
        System.out.println(authorization);

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(req, res);
            return;
        }

        String token = authorization.substring(7);

        if (jwtUtil.isExpired(token)) {
            filterChain.doFilter(req, res);
            return;
        }

        String loginId = jwtUtil.getUserIdFromToken(token);

        if (loginId != null) {
            BallBinUserDetails ballBinUserDetails = (BallBinUserDetails) ballBinUserDetailsService.loadUserByUsername(loginId);

            // Spring Security 인증 객체 생성
            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    ballBinUserDetails,
                    null,
                    ballBinUserDetails.getAuthorities()
            );

            // SecurityContext 에 인증 정보 저장
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(req, res);
    }
}
