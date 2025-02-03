package com.bb.ballBin.permission.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Data
@NoArgsConstructor
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false, unique = true, name = "permission_id")
    private String permissionId;

    @Column(unique = true, nullable = false, name = "permission_name")
    private String permissionName;

    @Column(name = "permission_description")
    private String permissionDescription;
}
