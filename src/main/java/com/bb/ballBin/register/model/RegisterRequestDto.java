package com.bb.ballBin.register.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class RegisterRequestDto {

    @Schema(description = "사용자 로그인 ID")
    private String loginId;
    @Schema(description = "사용자 비밀번호")
    private String loginPassword;
    @Schema(description = "사용자 명")
    private String userName;
    @Schema(description = "사용자 이메일")
    private String email;
    @Schema(description = "사용자 별칭")
    private String nickName;
    @Schema(description = "핸드폰 번호")
    private String phoneNumber;

    @Schema(description = "사용자 파일 필드")
    private MultipartFile userFile;
}
