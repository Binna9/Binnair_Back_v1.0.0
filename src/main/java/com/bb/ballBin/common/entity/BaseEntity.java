package com.bb.ballBin.common.entity;

import com.bb.ballBin.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
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
    @JoinColumn(name = "creator_id", referencedColumnName = "user_id")
    private User createdByUser;

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
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

        if (createdByUser != null) {
            this.creatorLoginId = createdByUser.getLoginId();
            this.creatorName = createdByUser.getUserName();
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
}
