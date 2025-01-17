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
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authorization= request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {

            System.out.println("token null");
            filterChain.doFilter(request, response);

            return;
        }

        String token = authorization.split(" ")[1];

        if (jwtUtil.isExpired(token)) {

            System.out.println("token expired");
            filterChain.doFilter(request, response);

            return;
        }

        String loginId = jwtUtil.getUsernameFromToken(token);

        BallBinUserDetails ballBinUserDetails = (BallBinUserDetails) ballBinUserDetailsService.loadUserByUsername(loginId);

        //스프링 시큐리티 인증 토큰 생성
        Authentication authToken = new UsernamePasswordAuthenticationToken(ballBinUserDetails, null, ballBinUserDetails.getAuthorities());
        //세션에 사용자 등록
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
