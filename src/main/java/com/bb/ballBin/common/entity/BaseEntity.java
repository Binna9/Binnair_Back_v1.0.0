package com.bb.ballBin.common.entity;

import com.bb.ballBin.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public class BaseEntity {

    @CreatedDate
    @Column(name = "create_datetime", nullable = false, updatable = false)
    private LocalDateTime createDatetime;

    @LastModifiedDate
    @Column(name = "modify_datetime")
    private LocalDateTime modifyDatetime;

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "creator_id", referencedColumnName = "user_id")
    private User createdByUser;

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "modifier_id", referencedColumnName = "user_id")
    private User modifiedByUser;

    @Column(name = "creator_login_id", nullable = false, updatable = false)
    private String creatorLoginId;

    @Column(name = "creator_name", nullable = false, updatable = false)
    private String creatorName;

    @Column(name = "modifier_login_id")
    private String modifierLoginId;

    @Column(name = "modifier_name")
    private String modifierName;

    @PrePersist
    public void prePersist() {

        this.modifyDatetime = null;

        if (hasField("views")) {
            initializeFieldIfExists("views", 0);
        }

        if (hasField("likes")) {
            initializeFieldIfExists("likes", 0);
        }

        if (hasField("unlikes")) {
            initializeFieldIfExists("unlikes", 0);
        }

        // 사용자 정보가 없는 경우 처리  todo: 시스템 계정 처리 필요
        if (createdByUser != null) {
            this.creatorLoginId = createdByUser.getLoginId();
            this.creatorName = createdByUser.getUserName();
        } else {
            this.creatorLoginId = "system";
            this.creatorName = "system";
        }

        this.modifiedByUser = null;
    }

    @PreUpdate
    public void preUpdate() {

        this.modifyDatetime = LocalDateTime.now();

        if (modifiedByUser != null) {
            this.modifierLoginId = modifiedByUser.getLoginId();
            this.modifierName = modifiedByUser.getUserName();
        }
    }

    private void initializeFieldIfExists(String fieldName, Object defaultValue) {
        try {
            Field field = this.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);

            if (field.get(this) == null) {
                field.set(this, defaultValue);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean hasField(String fieldName) {
        try {
            this.getClass().getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
