package com.bb.ballBin.user.entity;

import com.bb.ballBin.common.convert.BooleanToYNConverter;
import com.bb.ballBin.role.entity.Role;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false, unique = true)
    private String userId;

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String loginPassword;

    @Column(nullable = false)
    private String userName;

    @Column(unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String nickName;

    @Column
    private String phoneNumber;

    @Convert(converter = BooleanToYNConverter.class)
    private boolean isActive = true;

    private int failedLoginAttempts = 0;

    @ManyToMany
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
