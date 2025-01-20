package com.bb.ballBin.security.filter;

import com.bb.ballBin.security.jwt.BallBinUserDetails;
import com.bb.ballBin.security.jwt.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public LoginFilter(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 사용자가 제출한 요청에서 사용자 이름과 비밀번호를 추출하여 인증을 시도합니다.
     * @param req 클라이언트의 HTTP 요청
     * @param res 클라이언트의 HTTP 응답
     * @throws AuthenticationException 인증 실패 시 발생
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res) throws AuthenticationException {

        String userId = obtainUsername(req);
        String password = obtainPassword(req);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userId,  password, null);

        return authenticationManager.authenticate(authToken);
    }

    /**
     * 인증 성공 시 사용자 정보를 기반으로 JWT 토큰을 생성하고 응답 헤더에 추가합니다.
     * @param chain 필터 체인
     * @param auth 인증된 사용자 정보
     * @throws IOException 입출력 예외 발생 시
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res, FilterChain chain, Authentication auth) throws IOException {

        BallBinUserDetails ballBinUserDetails = (BallBinUserDetails) auth.getPrincipal();

        String userId = ballBinUserDetails.getUserId();

        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        String token = jwtUtil.createJwtToken(userId, roles, 60 * 60 * 10L); // 10시간 만료

        res.addHeader("Authorization", "Bearer " + token);
        res.setContentType("application/json");
        res.getWriter().write("{\"token\": \"" + token + "\"}");
    }

    /**
     * 인증 실패 시 HTTP 응답 상태를 401 (Unauthorized)로 설정합니다.
     * @param failed 인증 실패 예외
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest req, HttpServletResponse res, AuthenticationException failed) throws IOException {

        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\": \"Authentication failed\"}");
    }
}
