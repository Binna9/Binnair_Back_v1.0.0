package com.bb.ballBin.register.controller;

import com.bb.ballBin.register.model.RegisterDto;
import com.bb.ballBin.register.service.RegisterService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/register")
public class RegisterController {

    private final RegisterService registerService;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping("")
    public String registerProcess(@RequestBody RegisterDto registerDto) {
        registerService.registerProcess(registerDto);
        return "회원가입 성공";
    }
}
