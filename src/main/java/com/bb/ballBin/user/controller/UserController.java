package com.bb.ballBin.user.controller;

import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.model.UserRequsetDto;
import com.bb.ballBin.user.model.UserResponseDto;
import com.bb.ballBin.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

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

    @PutMapping("/{userId}")
    @Operation(summary = "사용자 수정")
    public ResponseEntity<String> modifyUser(@PathVariable String userId, @RequestBody UserRequsetDto userRequsetDto) {

        userService.updateUser(userId, userRequsetDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body("사용자 수정에 성공하셨습니다.");
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "사용자 삭제")
    public ResponseEntity<String> removeUser(@PathVariable String userId) {

        userService.deleteUser(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body("사용자 삭제에 성공하셨습니다.");
    }
}
