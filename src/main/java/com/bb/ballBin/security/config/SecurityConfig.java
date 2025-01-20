package com.bb.ballBin.security.config;

import com.bb.ballBin.security.filter.JwtFilter;
import com.bb.ballBin.security.filter.LoginFilter;
import com.bb.ballBin.security.jwt.BallBinUserDetailsService;
import com.bb.ballBin.security.jwt.util.JwtUtil;
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
import org.springframework.security.web.authentication.logout.LogoutFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final BallBinUserDetailsService ballBinUserDetailsService;
    private final SecurityPolicy securityPolicy;
    private final JwtUtil jwtUtil;

    public SecurityConfig(AuthenticationConfiguration authenticationConfiguration, BallBinUserDetailsService ballBinUserDetailsService, SecurityPolicy securityPolicy, JwtUtil jwtUtil) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.ballBinUserDetailsService = ballBinUserDetailsService;
        this.securityPolicy = securityPolicy;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Password 암호화
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security 설정
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable);  // CSRF 비활성
        http
                .httpBasic(AbstractHttpConfigurer::disable); // HttpBasic 비활성
        http
                .formLogin(AbstractHttpConfigurer::disable); // 로그인 Form 비활성
        http
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(securityPolicy.getPermittedUrls().toArray(String[]::new)).permitAll()
                        .requestMatchers("/admin").hasRole("ADMIN")
                        .anyRequest().authenticated()); // 명시되지 않은 모든 요청 인증 사용자 접근
        http
                .addFilterBefore(new JwtFilter(jwtUtil, ballBinUserDetailsService), LogoutFilter.class);
        http
                .addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), jwtUtil), UsernamePasswordAuthenticationFilter.class);
        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // 세션을 사용하지 않는 무상태 방식(STATELESS)

        return http.build();
    }
}
