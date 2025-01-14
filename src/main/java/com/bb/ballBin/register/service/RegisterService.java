package com.bb.ballBin.register.service;

import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.register.model.RegisterDto;
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

    public void registerProcess(RegisterDto registerDto) {

        String username = registerDto.getUserName();
        String password = registerDto.getPassword();
        String email = registerDto.getEmail();
        String nickName = registerDto.getNickName();

        Boolean isExist = userRepository.existsByUserName(username);
        if (isExist) {
            return;
        }

        User user = new User();

        user.setUserName(username);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        user.setEmail(email);
        user.setNickName(nickName);
        user.setRole("ROLE_ADMIN");

        userRepository.save(user);
    }
}
