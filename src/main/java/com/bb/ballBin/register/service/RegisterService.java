package com.bb.ballBin.register.service;

import com.bb.ballBin.common.annotation.CheckUserRegisterValid;
import com.bb.ballBin.file.entity.TargetType;
import com.bb.ballBin.file.service.FileService;
import com.bb.ballBin.user.entity.AuthProvider;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.register.model.RegisterRequestDto;
import com.bb.ballBin.user.mapper.UserMapper;
import com.bb.ballBin.user.model.UserRoleRequestDto;
import com.bb.ballBin.user.repository.UserRepository;
import com.bb.ballBin.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final UserService userService;
    private final FileService fileService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Transactional
    @CheckUserRegisterValid
    public void registerAccount(RegisterRequestDto registerRequestDto, List<MultipartFile> files) {

        String encodedPassword = bCryptPasswordEncoder.encode(registerRequestDto.getLoginPassword());
        registerRequestDto.setLoginPassword(encodedPassword);

        User user = userMapper.toEntity(registerRequestDto);
        user.setProvider(AuthProvider.LOCAL);
        user.setProviderId(registerRequestDto.getLoginId());
        user.setActive(false);

        userRepository.save(user);

        String userId = user.getUserId();

        UserRoleRequestDto userRoleRequestDto = new UserRoleRequestDto();
        userRoleRequestDto.setRoleName("ROLE_USER");

        userService.roleToUser(userId, userRoleRequestDto);

        if (files != null && !files.isEmpty()) {
            fileService.uploadFiles(TargetType.USER, userId, files);
        }
    }
}
