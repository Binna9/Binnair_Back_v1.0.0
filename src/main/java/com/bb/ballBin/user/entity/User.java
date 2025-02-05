package com.bb.ballBin.user.entity;

import com.bb.ballBin.common.convert.BooleanToYNConverter;
import com.bb.ballBin.common.entity.BaseEntity;
import com.bb.ballBin.role.entity.Role;
import com.bb.ballBin.role.model.RoleResponseDto;
import com.bb.ballBin.user.model.UserResponseDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false, unique = true, name = "user_id")
    private String userId;

    @Column(nullable = false, unique = true, name = "login_id")
    private String loginId;

    @Column(nullable = false, name = "login_password")
    private String loginPassword;

    @Column(nullable = false, name = "user_name")
    private String userName;

    @Column(unique = true, name = "email")
    private String email;

    @Column(nullable = false, unique = true, name = "nicK_name")
    private String nickName;

    @Column(name = "image_file_path")
    private String imageFilePath;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Builder.Default
    @Convert(converter = BooleanToYNConverter.class)
    @Column(name = "is_active")
    private boolean isActive = true;

    @Builder.Default
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Entity to DTO
     */
    public UserResponseDto toDto() {
        return UserResponseDto.builder()
                .loginId(this.loginId)
                .userName(this.userName)
                .email(this.email)
                .nickName(this.nickName)
                .phoneNumber(this.phoneNumber)
                .build();
    }
}
