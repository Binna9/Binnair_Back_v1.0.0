package com.bb.ballBin.role.entity;

import com.bb.ballBin.common.entity.BaseEntity;
import com.bb.ballBin.permission.entity.Permission;
import com.bb.ballBin.role.model.RoleResponseDto;
import com.bb.ballBin.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "roles")
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(updatable = false, nullable = false, unique = true, name ="role_id")
    private String roleId;

    @Column(unique = true, nullable = false, name = "role_name")
    private String roleName;

    @Column(name = "role_description")
    private String roleDescription;

    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}
