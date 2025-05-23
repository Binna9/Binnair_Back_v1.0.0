package com.bb.ballBin.security.jwt;

import com.bb.ballBin.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class BallBinUserDetails implements UserDetails {

    private final User user;

    /**
     * 사용자 역할 (Roles)을 Spring Security 의 GrantedAuthority 로 변환
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName())) // 또는 "ROLE_" + role.getRoleName()
                .collect(Collectors.toSet());
    }
    @Override
    public String getUsername() {
        return user.getUserName();
    }
    public String getUserId() {
        return user.getUserId();
    }
    public String getLoginId(){ return user.getLoginId(); }
    public String getEmail() {return user.getEmail();}
    public String getNickName() {return user.getNickName(); }
    public String getPhoneNumber() {return user.getPhoneNumber(); }
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
