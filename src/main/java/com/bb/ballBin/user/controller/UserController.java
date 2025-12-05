package com.bb.ballBin.user.controller;

import com.bb.ballBin.common.annotation.CurrentUserId;
import com.bb.ballBin.common.annotation.MessageKey;
import com.bb.ballBin.common.util.SecurityUtil;
import com.bb.ballBin.user.model.*;
import com.bb.ballBin.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final SecurityUtil securityUtil;

    @GetMapping("/fetch")
    @Operation(summary = "현재 로그인한 사용자 조회")
    public ResponseEntity<?> fetchUser() {
        return securityUtil.getCurrentUser();
    }

    @GetMapping("")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "사용자 전체 조회")
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(
            @PageableDefault(page = 0, size = 9, sort = "createDatetime", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "사용자 개별 조회")
    public ResponseEntity<UserResponseDto> userDetail(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @GetMapping("/image")
    @Operation(summary = "사용자 이미지 반환")
    public ResponseEntity<Resource> getProfileImage(@CurrentUserId String userId) {
        return userService.getUserImage(userId);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "사용자 수정")
    @MessageKey(value = "success.user.update")
    public ResponseEntity<Void> modifyUser(@PathVariable String userId, @RequestBody UserUpdateRequestDto userUpdateRequestDto) {

        userService.updateUser(userId, userUpdateRequestDto);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @Operation(summary = "사용자 삭제")
    @MessageKey(value = "success.user.delete")
    public ResponseEntity<Void> removeUser(@PathVariable String userId) {

        userService.deleteUser(userId);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/verify-password")
    @Operation(summary = "현재 비밀번호 검증")
    public ResponseEntity<Boolean> verifyPassword(@CurrentUserId String userId, @RequestBody Map<String, String> requestBody) {

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
    public ResponseEntity<Void> changePassword(@CurrentUserId String userId, @RequestBody UserPasswordChangeRequestDto passwordChangeDto) {

        userService.changePassword(userId, passwordChangeDto);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/change-active")
    @PreAuthorize("hasAuthority('USER_ACTIVE')")
    @Operation(summary = "사용자 활성화")
    @MessageKey(value = "success.user.active.change")
    public ResponseEntity<Void> changeActive(@RequestBody UserActiveRequestDto userActiveRequestDto) {

        userService.updateUserActivation(userActiveRequestDto);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/role")
    @Operation(summary = "사용자 역할 조회")
    public ResponseEntity<Set<String>> getUserRoles(@CurrentUserId String userId) {

        Set<String> roles = userService.getUserRoleNames(userId);

        return ResponseEntity.ok(roles);
    }

    @PostMapping("/assign-role")
    @Operation(summary = "사용자 역할 부여")
    @MessageKey(value = "success.user.role.assign")
    public ResponseEntity<Void> assignRoleToUser(@RequestBody List<UserRoleRequestDto> userRoleRequestDto) {

        userService.roleToUser(userRoleRequestDto);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/remove-role")
    @Operation(summary = "사용자 역할 제거")
    @MessageKey(value = "success.user.role.remove")
    public ResponseEntity<Void> removeRoleFromUser(@RequestBody List<UserRoleRequestDto> userRoleRequestDto) {

        userService.removeRoleFromUser(userRoleRequestDto);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
