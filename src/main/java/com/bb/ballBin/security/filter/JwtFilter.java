package com.bb.ballBin.security.filter;

import com.bb.ballBin.role.repository.RoleRepository;
import com.bb.ballBin.role.service.RoleService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final BallBinUserDetailsService ballBinUserDetailsService;
    private final RoleService roleService;
    private final RoleRepository roleRepository;
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
            // ✅ 1. JWT 에서 roles 꺼내기
            Set<String> roleNames = jwtUtil.getRolesFromToken(token, isOAuth2Token);

            Set<String> roleIds = roleRepository.findRoleIdsByRoleNames(roleNames);
            // ✅ 2. roles 기반으로 permissions 조회
            Set<String> permissionNames = roleService.getPermissionsByRoles(roleIds);

            // ✅ 3. permissions 를 GrantedAuthority 로 변환
            Set<GrantedAuthority> authorities = permissionNames.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());

            // ✅ 4. UserDetails 로드 (권한은 무시)
            BallBinUserDetails userDetails = (BallBinUserDetails) ballBinUserDetailsService.loadUserById(userId);

            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    authorities // ✅ 실제 권한 기반으로 설정
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);
        } else {
            System.out.println("❌ [JwtFilter] userId를 찾을 수 없음");
        }

        filterChain.doFilter(req, res);
    }
}
