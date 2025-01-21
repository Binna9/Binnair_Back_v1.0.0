package com.bb.ballBin.register.controller;

import com.bb.ballBin.register.model.RegisterRequestDto;
import com.bb.ballBin.register.service.RegisterService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/registers")
public class RegisterController {

    private final RegisterService registerService;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping("")
    @Operation(summary = "사용자 회원가입")
    public ResponseEntity<String> registerUser(@RequestBody RegisterRequestDto registerRequestDto) {

        registerService.registerAccount(registerRequestDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body("회원가입에 성공하셨습니다.");
    }
}
