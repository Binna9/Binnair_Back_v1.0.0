package com.bb.ballBin.user.service;

import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.model.UserRequsetDto;
import com.bb.ballBin.user.model.UserResponseDto;
import com.bb.ballBin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

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
                .orElseThrow(() -> new RuntimeException("error.user.notfound"));

        return user.toDto();
    }

    /**
     * 사용자 수정
     */
    public void updateUser(String userId , UserRequsetDto userRequsetDto){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("error.user.notfound"));

        userRepository.save(user);
    }

    /**
     * 사용자 삭제
     */
    public void deleteUser(String userId){

        userRepository.deleteById(userId);
    }
}
