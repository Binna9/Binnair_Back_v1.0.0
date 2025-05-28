package com.bb.ballBin.register.service;

import com.bb.ballBin.common.annotation.CheckUserRegisterValid;
import com.bb.ballBin.file.entity.TargetType;
import com.bb.ballBin.file.service.FileService;
import com.bb.ballBin.security.jwt.model.OAuthUserDto;
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
import java.util.UUID;

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

    /**
     * ✅ OAuth2 신규 사용자 회원가입 처리
     */
    @Transactional
    public User registerOAuthUser(OAuthUserDto userDto) {

        String loginId = userDto.getEmail();
        String dummyPassword = bCryptPasswordEncoder.encode(UUID.randomUUID().toString());

        User user = User.builder()
                .loginId(loginId)
                .loginPassword(dummyPassword)
                .provider(AuthProvider.GOOGLE)
                .providerId(userDto.getProviderId())
                .userName(userDto.getUserName())
                .email(userDto.getEmail())
                .isActive(true) // OAuth 사용자는 기본적으로 활성 상태
                .build();

        userRepository.save(user);

        UserRoleRequestDto userRoleRequestDto = new UserRoleRequestDto();
        userRoleRequestDto.setRoleName("ROLE_USER");

        userService.roleToUser(user.getUserId(), userRoleRequestDto);

        return user;
    }
}
