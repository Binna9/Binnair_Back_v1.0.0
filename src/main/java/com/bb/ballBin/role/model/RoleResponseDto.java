package com.bb.ballBin.role.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class RoleResponseDto {

    @Schema(description = "역할 명")
    private String roleName;
    @Schema(description = "역할 설명")
    private String roleDescription;
}
