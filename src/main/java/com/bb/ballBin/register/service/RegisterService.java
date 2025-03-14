package com.bb.ballBin.register.service;

import com.bb.ballBin.common.util.FileUtil;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.register.model.RegisterRequestDto;
import com.bb.ballBin.user.repository.UserRepository;
import com.bb.ballBin.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final FileUtil fileUtil;

    public ResponseEntity<String> registerAccount(RegisterRequestDto registerRequestDto, MultipartFile file) {

        userService.validatePassword(registerRequestDto.getLoginPassword());

        User user = new User();
        user.setLoginId(registerRequestDto.getLoginId());

        String bcryptHashedPassword = bCryptPasswordEncoder.encode(registerRequestDto.getLoginPassword());

        user.setLoginPassword(bcryptHashedPassword);
        user.setUserName(registerRequestDto.getUserName());
        user.setEmail(registerRequestDto.getEmail());
        user.setNickName(registerRequestDto.getNickName());
        user.setPhoneNumber(registerRequestDto.getPhoneNumber());

        user = userRepository.save(user);

        if (file != null && !file.isEmpty()) {
            String filePath = fileUtil.saveFile("user", user.getUserId(), file);
            user.setImageFilePath(filePath);
            userRepository.save(user);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료되었습니다.");
    }
}
