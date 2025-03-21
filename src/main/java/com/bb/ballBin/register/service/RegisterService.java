package com.bb.ballBin.register.service;

import com.bb.ballBin.file.entity.TargetType;
import com.bb.ballBin.file.service.FileService;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.register.model.RegisterRequestDto;
import com.bb.ballBin.user.mapper.UserMapper;
import com.bb.ballBin.user.repository.UserRepository;
import com.bb.ballBin.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private static final Logger logger = LoggerFactory.getLogger(RegisterService.class);

    private final UserService userService;
    private final FileService fileService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Transactional
    public void registerAccount(RegisterRequestDto registerRequestDto, List<MultipartFile> files) {
        try {
            userService.validatePassword(registerRequestDto.getLoginPassword());

            String encodedPassword = bCryptPasswordEncoder.encode(registerRequestDto.getLoginPassword());
            registerRequestDto.setLoginPassword(encodedPassword);

            User user = userMapper.toEntity(registerRequestDto);
            userRepository.save(user);

            String userId = user.getUserId();

            if (files != null && !files.isEmpty()) {
                fileService.uploadFiles(TargetType.USER, userId, files);
            }

        } catch (Exception e) {
            logger.error("오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("처리 중 오류 발생", e);
        }
    }
}
