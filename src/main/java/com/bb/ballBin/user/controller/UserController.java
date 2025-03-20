package com.bb.ballBin.user.controller;

import com.bb.ballBin.common.message.annotation.MessageKey;
import com.bb.ballBin.common.util.SecurityUtil;
import com.bb.ballBin.user.model.UserRequsetDto;
import com.bb.ballBin.user.model.UserResponseDto;
import com.bb.ballBin.user.model.UserPasswordChangeRequestDto;
import com.bb.ballBin.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping("")
    @Operation(summary = "사용자 전체 조회")
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(
            @PageableDefault(page = 0, size = 9, sort = "createDatetime", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "사용자 개별 조회")
    public ResponseEntity<UserResponseDto> userDetail(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @GetMapping("/image")
    @Operation(summary = "사용자 이미지 반환")
    public ResponseEntity<Resource> getProfileImage() {

        String userId = SecurityUtil.getCurrentUserId();

        return userService.getUserImage(userId);
    }

    @PutMapping("/{userId}")
    @Operation(summary = "사용자 수정")
    @MessageKey(value = "success.update")
    public ResponseEntity<String> modifyUser(@PathVariable String userId, @RequestBody UserRequsetDto userRequsetDto) {

        userService.updateUser(userId, userRequsetDto);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "사용자 삭제")
    @MessageKey(value = "success.delete")
    public ResponseEntity<String> removeUser(@PathVariable String userId) {

        userService.deleteUser(userId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-password")
    @Operation(summary = "현재 비밀번호 검증")
    public ResponseEntity<Boolean> verifyPassword(@RequestBody Map<String, String> requestBody) {
        String userId = SecurityUtil.getCurrentUserId();

        String currentPassword = requestBody.get("password");

        if (currentPassword == null || currentPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(false);
        }

        boolean isMatch = userService.verifyCurrentPassword(userId, currentPassword);

        return ResponseEntity.ok(isMatch);
    }

    @PutMapping("/change-password")
    @Operation(summary = "사용자 비밀번호 변경")
    @MessageKey(value = "success.user.password.change")
    public ResponseEntity<String> changePassword(@RequestBody UserPasswordChangeRequestDto passwordChangeDto) {

        String userId = SecurityUtil.getCurrentUserId();
        userService.changePassword(userId, passwordChangeDto);

        return ResponseEntity.ok().build();
    }
}
