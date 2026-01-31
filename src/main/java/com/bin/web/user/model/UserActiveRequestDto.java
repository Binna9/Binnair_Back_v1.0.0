package com.bin.web.user.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserActiveRequestDto {

    @Schema(name = "사용자 ID")
    private String userId;
    @Schema(name = "계정 활성화 여부")
    private boolean active;
}
