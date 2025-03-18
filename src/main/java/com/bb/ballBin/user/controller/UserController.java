package com.bb.ballBin.user.controller;

import com.bb.ballBin.common.message.annotation.MessageKey;
import com.bb.ballBin.common.util.FileUtil;
import com.bb.ballBin.common.util.SecurityUtil;
import com.bb.ballBin.user.model.UserRequsetDto;
import com.bb.ballBin.user.model.UserResponseDto;
import com.bb.ballBin.user.model.UserPasswordChangeRequestDto;
import com.bb.ballBin.user.repository.UserRepository;
import com.bb.ballBin.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/image")
    @Operation(summary = "사용자 이미지 반환")
    public ResponseEntity<Resource> getProfileImage() {

        String userId = SecurityUtil.getCurrentUserId();

        return userRepository.findById(userId)
                .map(user -> {
                    String relativePath = user.getFilePath();
                    if (relativePath == null || relativePath.isEmpty()) {
                        System.out.println("❌ No image path found for user: " + userId);
                        return ResponseEntity.notFound().<Resource>build();
                    }
                    return fileUtil.getImageResponse("user", relativePath);
                })
                .orElseGet(() -> ResponseEntity.notFound().<Resource>build());
    }

    @PutMapping("/{userId}")
    @Operation(summary = "사용자 수정")
    @MessageKey(value = "success.update")
    public ResponseEntity<String> modifyUser(@PathVariable String userId, @RequestBody UserRequsetDto userRequsetDto) {

        userService.updateUser(userId, userRequsetDto);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/image-upload")
    @Operation(summary = "사용자 프로필 이미지 업로드")
    @MessageKey(value = "success.create")
    public ResponseEntity<String> uploadProfileImage(@RequestParam(value = "file", required = false) MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("❌ 업로드할 파일이 없습니다.");
        }

        String userId = SecurityUtil.getCurrentUserId();

        return userRepository.findById(userId).map(user -> {
            try {
                // 기존 이미지 삭제 (기존 파일이 있을 경우)
                if (user.getFilePath() != null) {
                    fileUtil.deleteFile("user", user.getFilePath());
                }
                // 새 이미지 저장
                String savedPath = fileUtil.saveFile("user", userId, file);
                // 사용자 정보 업데이트
                user.setFilePath(savedPath);
                userRepository.save(user);

                return ResponseEntity.ok("✅ 프로필 이미지 업로드 성공: " + savedPath);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("❌ 프로필 이미지 업로드 실패: " + e.getMessage());
            }
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("❌ 사용자를 찾을 수 없습니다."));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "사용자 삭제")
    @MessageKey(value = "success.delete")
    public ResponseEntity<String> removeUser(@PathVariable String userId) {

        userService.deleteUser(userId);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * ✅ 현재 비밀번호 확인 API
     */
    @PostMapping("/verify-password")
    @Operation(summary = "현재 비밀번호 검증")
    public ResponseEntity<Boolean> verifyPassword(@RequestBody Map<String, String> requestBody) {
        String userId = SecurityUtil.getCurrentUserId();

        // 요청 본문에서 비밀번호 추출
        String currentPassword = requestBody.get("password");

        if (currentPassword == null || currentPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(false);
        }

        boolean isMatch = userService.verifyCurrentPassword(userId, currentPassword);
        return ResponseEntity.ok(isMatch);
    }

    /**
     * ✅ 비밀번호 변경 API
     */
    @PutMapping("/change-password")
    @Operation(summary = "사용자 비밀번호 변경")
    @MessageKey(value = "success.user.password.change")
    public ResponseEntity<String> changePassword(@RequestBody UserPasswordChangeRequestDto passwordChangeDto) {

        String userId = SecurityUtil.getCurrentUserId();
        userService.changePassword(userId, passwordChangeDto);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
