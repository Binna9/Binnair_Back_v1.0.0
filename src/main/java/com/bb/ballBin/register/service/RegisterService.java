package com.bb.ballBin.register.service;

import com.bb.ballBin.common.util.FileUtil;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.register.model.RegisterRequestDto;
import com.bb.ballBin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final FileUtil fileUtil;

    public void registerAccount(RegisterRequestDto registerRequestDto, MultipartFile file) {

        User user = new User();

        user.setLoginId(registerRequestDto.getLoginId());
        user.setLoginPassword(bCryptPasswordEncoder.encode(registerRequestDto.getLoginPassword()));
        user.setUserName(registerRequestDto.getUserName());
        user.setEmail(registerRequestDto.getEmail());
        user.setNickName(registerRequestDto.getNickName());
        user.setPhoneNumber(registerRequestDto.getPhoneNumber());

        user = userRepository.save(user);

        if (file != null && !file.isEmpty()) {
            String filePath = fileUtil.saveFile(user.getUserId(), file);
            user.setImageFilePath(filePath);
            userRepository.save(user);
        }
    }
}
