package com.bin.web.user.entity;

import com.bin.web.bookmark.entity.Bookmark;
import com.bin.web.common.convert.BooleanToYNConverter;
import com.bin.web.common.entity.BaseEntity;
import com.bin.web.role.entity.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Builder
@EqualsAndHashCode(callSuper = false, exclude = {"bookmarks", "roles"})
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "providerId"})
})
public class User extends BaseEntity {

    @Id
    @org.hibernate.annotations.UuidGenerator(style = org.hibernate.annotations.UuidGenerator.Style.TIME)
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

    @Column(name = "phone_number")
    private String phoneNumber;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Bookmark> bookmarks = new HashSet<>();

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider; // ✅ 로그인 제공자

    @Column(nullable = false, unique = true, name = "provider_id")
    private String providerId; // ✅ 플랫폼별 유일한 식별자
}
