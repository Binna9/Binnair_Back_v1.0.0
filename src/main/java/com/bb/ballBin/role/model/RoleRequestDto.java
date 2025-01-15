package com.bb.ballBin.role.model;

import com.bb.ballBin.role.entity.Role;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@ToString
@Builder
public class RoleRequestDto {

    private String roleName;
    private String roleDescription;

    public Role toEntity() {
        return Role.builder()
                .roleName(this.roleName)
                .roleDescription(this.roleDescription)
                .build();
    }
}
