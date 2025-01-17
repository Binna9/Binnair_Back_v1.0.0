package com.bb.ballBin.security.jwt;

import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class BallBinUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public BallBinUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("해당 로그인 ID를 찾을 수 없습니다: " + loginId));

        return new BallBinUserDetails(user);
    }
}
