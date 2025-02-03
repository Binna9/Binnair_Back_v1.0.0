package com.bb.ballBin.security.config;

import com.bb.ballBin.common.message.Service.MessageService;
import com.bb.ballBin.security.filter.JwtFilter;
import com.bb.ballBin.security.filter.LoginFilter;
import com.bb.ballBin.security.jwt.BallBinUserDetailsService;
import com.bb.ballBin.security.jwt.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final SecurityPolicy securityPolicy;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final BallBinUserDetailsService ballBinUserDetailsService;

    public SecurityConfig(SecurityPolicy securityPolicy, MessageService messageService, ObjectMapper objectMapper, JwtUtil jwtUtil, BallBinUserDetailsService ballBinUserDetailsService) {
        this.securityPolicy = securityPolicy;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
        this.ballBinUserDetailsService = ballBinUserDetailsService;
    }

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {

        JwtFilter jwtFilter = new JwtFilter(jwtUtil, ballBinUserDetailsService);
        LoginFilter loginFilter = new LoginFilter(authenticationManager, messageService, objectMapper, jwtUtil);

        loginFilter.setFilterProcessesUrl("/login");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(securityPolicy.getPermittedUrls().toArray(String[]::new)).permitAll()
                        .requestMatchers("/admin").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, LogoutFilter.class)
                .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
