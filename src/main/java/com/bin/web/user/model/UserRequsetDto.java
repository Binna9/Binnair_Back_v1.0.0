package com.bin.web.user.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserRequsetDto {

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
}
