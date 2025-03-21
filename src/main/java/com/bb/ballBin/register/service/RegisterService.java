package com.bb.ballBin.register.service;

import com.bb.ballBin.common.exception.InvalidPasswordException;
import com.bb.ballBin.file.entity.TargetType;
import com.bb.ballBin.file.service.FileService;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.register.model.RegisterRequestDto;
import com.bb.ballBin.user.mapper.UserMapper;
import com.bb.ballBin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final FileService fileService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Transactional
    public void registerAccount(RegisterRequestDto registerRequestDto, List<MultipartFile> files) {

        validatePasswordMatch(registerRequestDto.getLoginPassword(), registerRequestDto.getConfirmPassword());
        validatePassword(registerRequestDto.getLoginPassword());

        String encodedPassword = bCryptPasswordEncoder.encode(registerRequestDto.getLoginPassword());
        registerRequestDto.setLoginPassword(encodedPassword);

        User user = userMapper.toEntity(registerRequestDto);
        userRepository.save(user);

        String userId = user.getUserId();

        if (files != null && !files.isEmpty()) {
            fileService.uploadFiles(TargetType.USER, userId, files);
        }
    }

    /**
     * 비밀번호 검증 및 검증 실패 시 상세 메시지 반환
     */
    public void validatePassword(String password) {
        if (password.length() < 8) {
            throw new InvalidPasswordException("error.password1");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new InvalidPasswordException("error.password2");
        }
        if (!password.matches(".*[a-zA-Z].*")) {
            throw new InvalidPasswordException("error.password3");
        }
        if (!password.matches(".*[!@#$%^&*()_+=-].*")) {
            throw new InvalidPasswordException("error.password4");
        }
    }

    /**
     * Password Confirm
     */
    public void validatePasswordMatch(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new InvalidPasswordException("error.password.mismatch");
        }
    }
}
