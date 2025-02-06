package com.bb.ballBin.security.config;

import com.bb.ballBin.security.filter.JwtFilter;
import com.bb.ballBin.security.jwt.BallBinUserDetailsService;
import com.bb.ballBin.security.jwt.service.JwtBlacklistService;
import com.bb.ballBin.security.jwt.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityPolicy securityPolicy;
    private final JwtUtil jwtUtil;
    private final BallBinUserDetailsService ballBinUserDetailsService;
    private final JwtBlacklistService jwtBlacklistService;

    /**
     * Password 암호화
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager Bean 등록
     */
    @Bean
    public AuthenticationManager authenticationManagerBean(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Security 설정
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        JwtFilter jwtFilter = new JwtFilter(jwtUtil, ballBinUserDetailsService, jwtBlacklistService);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(securityPolicy.getPermittedUrls().toArray(String[]::new)).permitAll() // ✅ 인증 없이 접근 가능
                        .requestMatchers(securityPolicy.getAuthenticatedUrls().toArray(String[]::new)).authenticated() // ✅ 인증 필요
                        .requestMatchers(securityPolicy.getAdminUrls().toArray(String[]::new)).hasRole("ADMIN") // ✅ 관리자 권한 필요
                        .anyRequest().denyAll() // ✅ 나머지 요청은 차단
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // ✅ 필터 순서 변경
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
