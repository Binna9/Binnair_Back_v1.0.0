package com.bb.ballBin.register.service;

import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.register.model.RegisterUserRequestDto;
import com.bb.ballBin.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public RegisterService(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public void registerAccount(RegisterUserRequestDto registerUserRequestDto) {

        String loginId = registerUserRequestDto.getLoginId();
        String loginPassword = registerUserRequestDto.getLoginPassword();
        String userName = registerUserRequestDto.getUserName();
        String email = registerUserRequestDto.getEmail();
        String nickName = registerUserRequestDto.getNickName();

        User user = new User();

        user.setLoginId(loginId);
        user.setLoginPassword(bCryptPasswordEncoder.encode(loginPassword));
        user.setUserName(userName);
        user.setEmail(email);
        user.setNickName(nickName);

        userRepository.save(user);
    }
}
