package com.bb.ballBin.user.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPasswordChangeRequestDto {

    @Schema(description = "현재 비밀번호")
    private String currentPassword;

    @Schema(description = "새로운 비밀번호")
    private String newPassword;

    @Schema(description = "새로운 비밀번호 확인")
    private String confirmPassword;
}
