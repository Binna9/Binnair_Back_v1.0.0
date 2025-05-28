package com.bb.ballBin.user.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {

    @Schema(description = "사용자 ID")
    private String userId;
    @Schema(description = "사용자 로그인 ID")
    private String loginId;
    @Schema(description = "사용자 명")
    private String userName;
    @Schema(description = "사용자 이메일")
    private String email;
    @Schema(description = "사용자 별칭")
    private String nickName;
    @Schema(description = "핸드폰 번호")
    private String phoneNumber;
    @Schema(description = "계정 활성화 상태")
    private boolean isActive;
    @Schema(description = "사용자 역할")
    private Set<String> roles;
}
