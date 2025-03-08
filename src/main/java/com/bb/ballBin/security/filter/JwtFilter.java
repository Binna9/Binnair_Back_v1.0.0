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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final BallBinUserDetailsService ballBinUserDetailsService;
    private final JwtBlacklistService jwtBlacklistService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authorization = req.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(req, res);
            return;
        }

        String token = authorization.substring(7);
        boolean isOAuth2Token = req.getRequestURI().startsWith("/google"); // ✅ OAuth2 로그인 여부 체크

        // ✅ 1. 블랙리스트된 토큰인지 확인
        if (jwtBlacklistService.isBlacklisted(token)) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.getWriter().write("{\"error\":\"TOKEN_BLACKLISTED\"}");
            return;
        }

        // ✅ 2. Access Token 만료 체크 (401 + ACCESS_TOKEN_EXPIRED 반환)
        if (jwtUtil.isExpired(token, isOAuth2Token)) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"ACCESS_TOKEN_EXPIRED\"}");
            return;
        }

        String userId = jwtUtil.getUserIdFromToken(token, isOAuth2Token);

        if (userId != null) {
            BallBinUserDetails ballBinUserDetails = (BallBinUserDetails) ballBinUserDetailsService.loadUserById(userId);

            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    ballBinUserDetails,
                    null,
                    ballBinUserDetails.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);
        } else {
            System.out.println("❌ [JwtFilter] userId를 찾을 수 없음");
        }

        filterChain.doFilter(req, res);
    }
}
