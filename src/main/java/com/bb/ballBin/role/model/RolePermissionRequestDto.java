package com.bb.ballBin.role.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class RolePermissionRequestDto {

    @Schema(name = "역할 ID")
    private String roleId;
    @Schema(name = "권한 명")
    private String permissionName;
}
