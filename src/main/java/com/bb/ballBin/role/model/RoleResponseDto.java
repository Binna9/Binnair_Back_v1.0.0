package com.bb.ballBin.role.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@ToString
@Builder
public class RoleResponseDto {

    private String roleName;
    private String roleDescription;
}
