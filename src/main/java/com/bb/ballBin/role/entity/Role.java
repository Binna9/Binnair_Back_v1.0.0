package com.bb.ballBin.role.entity;

import com.bb.ballBin.role.model.RoleResponseDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Data
@NoArgsConstructor
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false, unique = true)
    private String roleId;

    @Column(unique = true, nullable = false)
    private String roleName;

    private String roleDescription;

    @Builder
    @SuppressWarnings("unused")
    public Role(String roleId, String roleName, String roleDescription) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.roleDescription = roleDescription;
    }

    public RoleResponseDto toDto() {
        return RoleResponseDto.builder()
                .roleName(this.roleName)
                .roleDescription(this.roleDescription)
                .build();
    }
}
