package com.bin.web.user.service;

import com.bin.web.common.annotation.CheckUserChangePasswordValid;
import com.bin.web.common.exception.ImmutableFieldException;
import com.bin.web.common.exception.InvalidPasswordException;
import com.bin.web.common.exception.NotFoundException;
import com.bin.web.common.util.FileUtil;
import com.bin.web.file.entity.File;
import com.bin.web.file.entity.TargetType;
import com.bin.web.file.repository.FileRepository;
import com.bin.web.file.service.FileService;
import com.bin.web.role.entity.Role;
import com.bin.web.role.repository.RoleRepository;
import com.bin.web.user.entity.AuthProvider;
import com.bin.web.user.entity.User;
import com.bin.web.user.mapper.UserMapper;
import com.bin.web.user.model.*;
import com.bin.web.user.model.*;
import com.bin.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileService fileService;
    private final FileUtil fileUtil;
    private final FileRepository fileRepository;

    /**
     * providerId로 사용자 조회
     */
    public Optional<User> findByProviderId(String providerId) {
        return userRepository.findByProviderId(providerId);
    }

    /**
     * 사용자 전체 조회
     */
    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toDto);
    }

    /**
     * 사용자 개별 조회
     */
    public UserResponseDto getUserById(String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("error.user.notfound"));

        return userMapper.toDto(user);
    }

    /**
     * 사용자 이미지 반환
     */
    public ResponseEntity<Resource> getUserImage(String userId) {

        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("error.user.notfound"));

        List<File> files = fileRepository.findByTargetIdAndTargetType(userId, TargetType.USER);

        String relativePath = null;

        if (files != null && !files.isEmpty()) {
            relativePath = files.get(0).getFilePath();
        }

        return fileUtil.getImageResponse("user", relativePath);
    }


    /**
     * 사용자 수정
     */
    public void updateUser(String userId, UserUpdateRequestDto userUpdateRequestDto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("error.user.notfound"));

        if (user.getProvider() != null && user.getProviderId() != null) {
            if (Objects.equals(user.getProvider(), AuthProvider.GOOGLE)) {
                throw new ImmutableFieldException("error.user.provider.immutable");
            }
        }

        userMapper.updateEntity(userUpdateRequestDto, user);

        userRepository.save(user);
    }

    /**
     * 사용자 삭제
     */
    public void deleteUser(String userId) {

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("error.user.notfound"));

        userRepository.deleteById(userId);

        fileService.deleteFilesByTarget(TargetType.PRODUCT, userId);
    }

    /**
     * 사용자 비밀번호 검증
     */
    public boolean verifyCurrentPassword(String userId, String currentPassword) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("error.user.notfound"));

        String hashedPasswordFromDB = user.getLoginPassword(); // DB에 저장된 해싱된 비밀번호

        if (!passwordEncoder.matches(currentPassword, hashedPasswordFromDB)) {
            throw new InvalidPasswordException(("error.password.incorrect"));
        }

        return true;
    }

    /**
     * 사용자 비밀번호 변경
     */
    @CheckUserChangePasswordValid
    public void changePassword(String userId, UserPasswordChangeRequestDto passwordChangeDto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("error.user.notfound"));

        String bcryptHashedPassword = passwordEncoder.encode(passwordChangeDto.getNewPassword());

        user.setLoginPassword(bcryptHashedPassword);
        userRepository.save(user);
    }

    /**
     * 사용자 계정 활성/비활성화
     */
    public void updateUserActivation(UserActiveRequestDto userActiveRequestDto) {

        User user = userRepository.findById(userActiveRequestDto.getUserId())
                .orElseThrow(() -> new NotFoundException("error.user.notfound"));

        user.setActive(userActiveRequestDto.isActive());
        userRepository.save(user);
    }

    /**
     * 사용자 역할 조회
     */
    public Set<String> getUserRoleNames(String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return user.getRoles().stream()
                .map(Role::getRoleName) // 또는 getRoleId()
                .collect(Collectors.toSet());
    }

    /**
     * 사용자 역할 부여
     */
    public void roleToUser(List<UserRoleRequestDto> userRoleRequestDtoList) {

        for (UserRoleRequestDto dto : userRoleRequestDtoList) {

            User user = userRepository.findByUserId(dto.getUserId())
                    .orElseThrow(() -> new NotFoundException("error.user.notfound"));

            Role role = roleRepository.findByRoleNameIgnoreCase(dto.getRoleName())
                    .orElseThrow(() -> new NotFoundException("error.role.notfound"));

            boolean exists = userRepository.existsUserRole(user.getUserId(), role.getRoleId());

            if (!exists) {
                userRepository.insertUserRole(user.getUserId(), role.getRoleId());
            }
        }
    }

    /**
     * 사용자 역할 삭제
     */
    public void removeRoleFromUser(List<UserRoleRequestDto> userRoleRequestDtoList) {

        for (UserRoleRequestDto dto : userRoleRequestDtoList) {

            User user = userRepository.findByUserId(dto.getUserId())
                    .orElseThrow(() -> new NotFoundException("error.user.notfound"));

            Role role = roleRepository.findByRoleNameIgnoreCase(dto.getRoleName())
                    .orElseThrow(() -> new NotFoundException("error.role.notfound"));

            userRepository.deleteUserRole(user.getUserId(), role.getRoleId());
        }
    }
}
