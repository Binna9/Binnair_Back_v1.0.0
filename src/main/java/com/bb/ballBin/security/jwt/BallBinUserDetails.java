package com.bb.ballBin.security.jwt;

import com.bb.ballBin.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

public class BallBinUserDetails implements UserDetails {

    private final User user;

    public BallBinUserDetails(User user) {
        this.user = user;
    }

    /**
     * 사용자 역할 (Roles)을 Spring Security 의 GrantedAuthority 로 변환
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }

    @Override
    public String getUsername() {
        return user.getLoginId();
    }

    public String getUserId() {
        return user.getUserId();
    }

    public String getLoginId(){
        return user.getLoginId();
    }

    @Override
    public String getPassword() {
        return user.getLoginPassword();
    }

    /**
     * 계정 만료 여부
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 계정 잠금 여부 (5회)
     */
    @Override
    public boolean isAccountNonLocked() {
        return user.getFailedLoginAttempts() < 5;
    }

    /**
     * 자격 증명 만료 여부
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 계정 활성화 여부
     */
    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
