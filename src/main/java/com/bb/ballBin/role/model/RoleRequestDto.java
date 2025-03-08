package com.bb.ballBin.role.model;

import com.bb.ballBin.role.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class RoleRequestDto {

    @Schema(description = "역할 명")
    private String roleName;
    @Schema(description = "역할 설명")
    private String roleDescription;

    public Role toEntity() {
        return Role.builder()
                .roleName(this.roleName)
                .roleDescription(this.roleDescription)
                .build();
    }
}
