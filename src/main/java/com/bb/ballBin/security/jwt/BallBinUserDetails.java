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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return user.getRoles().stream()
                .map(SimpleGrantedAuthority::new) // roles 에서 각 String 을 SimpleGrantedAuthority 로 변환
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return user.getLoginPassword();
    }

    @Override
    public String getUsername() {
        return user.getLoginId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getFailedLoginAttempts() < 5;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
