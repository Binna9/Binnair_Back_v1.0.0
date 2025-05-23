package com.bb.ballBin.permission.entity;

import com.bb.ballBin.common.entity.BaseEntity;
import com.bb.ballBin.role.entity.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true , callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "permissions")
public class Permission extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    @org.hibernate.annotations.UuidGenerator(style = org.hibernate.annotations.UuidGenerator.Style.TIME)
    @Column(updatable = false, nullable = false, unique = true, name = "permission_id")
    private String permissionId;

    @Column(unique = true, nullable = false, name = "permission_name")
    private String permissionName;

    @Column(name = "permission_description")
    private String permissionDescription;

    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();
}
