package com.bb.ballBin.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

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
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false, unique = true)
    private String nickName;
    @Column(nullable = false)
    private boolean isActive;

    private int failedLoginAttempts = 0;

    @ElementCollection
    private Set<String> roles;
}
