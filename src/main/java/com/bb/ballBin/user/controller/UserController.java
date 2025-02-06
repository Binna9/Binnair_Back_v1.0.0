package com.bb.ballBin.user.controller;

import com.bb.ballBin.common.message.annotation.MessageKey;
import com.bb.ballBin.common.util.FileUtil;
import com.bb.ballBin.user.model.UserRequsetDto;
import com.bb.ballBin.user.model.UserResponseDto;
import com.bb.ballBin.user.repository.UserRepository;
import com.bb.ballBin.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final FileUtil fileUtil;

    @GetMapping("")
    @Operation(summary = "사용자 전체 조회")
    public ResponseEntity<List<UserResponseDto>> userList() {

        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{userId}")
    @Operation(summary = "사용자 개별 조회")
    public ResponseEntity<UserResponseDto> userDetail(@PathVariable String userId) {

        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @GetMapping("/{userId}/image")
    @Operation(summary = "사용자 이미지 반환")
    public ResponseEntity<Resource> getProfileImage(@PathVariable String userId) {

        return userRepository.findById(userId)
                .map(user -> fileUtil.getProfileImageResponse(user.getImageFilePath()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    @Operation(summary = "사용자 수정")
    @MessageKey(value = "success.user.update")
    public ResponseEntity<String> modifyUser(@PathVariable String userId, @RequestBody UserRequsetDto userRequsetDto) {

        userService.updateUser(userId, userRequsetDto);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "사용자 삭제")
    @MessageKey(value = "success.user.delete")
    public ResponseEntity<String> removeUser(@PathVariable String userId) {

        userService.deleteUser(userId);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
