package com.bb.ballBin.register.service;

import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.register.model.RegisterRequestDto;
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

    public void registerAccount(RegisterRequestDto registerRequestDto) {

        String loginId = registerRequestDto.getLoginId();
        String loginPassword = registerRequestDto.getLoginPassword();
        String userName = registerRequestDto.getUserName();
        String email = registerRequestDto.getEmail();
        String nickName = registerRequestDto.getNickName();
        String phoneNumber = registerRequestDto.getPhoneNumber();

        User user = new User();

        user.setLoginId(loginId);
        user.setLoginPassword(bCryptPasswordEncoder.encode(loginPassword));
        user.setUserName(userName);
        user.setEmail(email);
        user.setNickName(nickName);
        user.setPhoneNumber(phoneNumber);

        userRepository.save(user);
    }
}
