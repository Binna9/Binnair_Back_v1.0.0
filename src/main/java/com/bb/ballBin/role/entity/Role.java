package com.bb.ballBin.role.entity;

import com.bb.ballBin.common.entity.BaseEntity;
import com.bb.ballBin.permission.entity.Permission;
import com.bb.ballBin.role.model.RoleResponseDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "roles")
public class Role extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false, unique = true, name ="role_id")
    private String roleId;

    @Column(unique = true, nullable = false, name = "role_name")
    private String roleName;

    @Column(name = "role_description")
    private String roleDescription;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    /**
     * Entity to DTO
     */
    public RoleResponseDto toDto() {
        return RoleResponseDto.builder()
                .roleName(this.roleName)
                .roleDescription(this.roleDescription)
                .build();
    }
}
