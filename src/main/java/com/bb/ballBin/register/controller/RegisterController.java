package com.bb.ballBin.register.controller;

import com.bb.ballBin.common.message.annotation.MessageKey;
import com.bb.ballBin.register.model.RegisterRequestDto;
import com.bb.ballBin.register.service.RegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/registers")
public class RegisterController {

    private final RegisterService registerService;

    @PostMapping("")
    @MessageKey(value = "success.user.register")
    @Operation(summary = "사용자 회원가입",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(implementation = RegisterRequestDto.class)
                    )
            ))
    public ResponseEntity<Void> registerUser(@ModelAttribute RegisterRequestDto registerRequestDto,
                                               @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        registerService.registerAccount(registerRequestDto, files);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
