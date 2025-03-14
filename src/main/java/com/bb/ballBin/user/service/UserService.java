package com.bb.ballBin.user.service;

import com.bb.ballBin.common.exception.ImmutableFieldException;
import com.bb.ballBin.common.exception.InvalidPasswordException;
import com.bb.ballBin.common.exception.NotFoundException;
import com.bb.ballBin.role.entity.Role;
import com.bb.ballBin.role.repository.RoleRepository;
import com.bb.ballBin.security.jwt.model.OAuthUserDto;
import com.bb.ballBin.user.entity.AuthProvider;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.model.UserPasswordChangeRequestDto;
import com.bb.ballBin.user.model.UserRequsetDto;
import com.bb.ballBin.user.model.UserResponseDto;
import com.bb.ballBin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * ✅ providerId로 사용자 조회 (OAuth 로그인용) todo: 구현 구체화 필요
     */
    public Optional<User> findByProviderId(String providerId) {
        return userRepository.findByProviderId(providerId);
    }

    /**
     * ✅ OAuth2 신규 사용자 회원가입 처리 todo: 구현 구체화 필요
     */
    public User registerOAuthUser(OAuthUserDto userDto) {

        Set<Role> roles = new HashSet<>(roleRepository.findByRoleNameIn(List.of("ROLE_USER")));
        String dummyPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        User newUser = User.builder()
                .loginId(userDto.getEmail()) // ✅ 이메일을 로그인 ID로 사용
                .loginPassword(dummyPassword)
                .provider(AuthProvider.GOOGLE) // ✅ 로그인 제공자 (GOOGLE)
                .providerId(userDto.getProviderId()) // ✅ Google 제공 ID
                .userName(userDto.getUserName()) // ✅ 사용자 이름
                .email(userDto.getEmail()) // ✅ 이메일
                .imageFilePath(userDto.getProfileImageUrl()) // ✅ 프로필 이미지
                .isActive(true) // ✅ 기본 활성 상태
                .roles(roles) // ✅ 기본 역할 설정
                .build();

        return userRepository.save(newUser);
    }

    /**
     * 사용자 전체 조회
     */
    public List<UserResponseDto> getAllUsers() {

        return userRepository.findAll().stream()
                .map(User::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 개별 조회
     */
    public UserResponseDto getUserById(String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("error.user.notfound"));

        return user.toDto();
    }

    /**
     * 사용자 수정
     */
    public void updateUser(String userId, UserRequsetDto userRequsetDto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("error.user.notfound"));

        if (!Objects.equals(user.getLoginId(), userRequsetDto.getLoginId())) {
            throw new ImmutableFieldException("error.user.loginId.immutable");
        }
        // OAuth 사용자일 경우 provider 및 providerId 변경 방지
        if (user.getProvider() != null && user.getProviderId() != null) {
            if (!Objects.equals(user.getProvider(), AuthProvider.GOOGLE)) {
                throw new ImmutableFieldException("error.user.provider.immutable");
            }
        }

        user.setUserName(userRequsetDto.getUserName());
        user.setEmail(userRequsetDto.getEmail());
        user.setNickName(userRequsetDto.getNickName());
        user.setPhoneNumber(userRequsetDto.getPhoneNumber());

        if (userRequsetDto.getLoginPassword() != null && !userRequsetDto.getLoginPassword().isEmpty()) {
            user.setLoginPassword(passwordEncoder.encode(userRequsetDto.getLoginPassword()));
        }

        // 저장
        userRepository.save(user);
    }

    /**
     * 사용자 삭제
     */
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }


    public Set<String> getUserRoles(String userId) {
        return Set.of("ROLE_USER"); // todo :: 기본값으로 ROLE_USER 반환 (실제 DB 조회 필요)
    }

    /**
     * 비밀번호 검증
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
     * 비밀번호 변경
     */
    public void changePassword(String userId, UserPasswordChangeRequestDto passwordChangeDto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("error.user.notfound"));

        validatePassword(passwordChangeDto.getNewPassword());

        if (!passwordChangeDto.getNewPassword().equals(passwordChangeDto.getConfirmPassword())) {
            throw new InvalidPasswordException("error.password.mismatch");
        }

        String bcryptHashedPassword = passwordEncoder.encode(passwordChangeDto.getNewPassword());

        user.setLoginPassword(bcryptHashedPassword);
        userRepository.save(user);
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
}
