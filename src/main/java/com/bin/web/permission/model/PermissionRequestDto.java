package com.bin.web.permission.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class PermissionRequestDto {

    @Schema(description = "권한 명")
    private String permissionName;
    @Schema(description = "권한 설명")
    private String permissionDescription;
}
