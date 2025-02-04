package com.bb.ballBin.common.entity;

import com.bb.ballBin.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "생성 일시")
    private LocalDateTime createDatetime;

    @LastModifiedDate
    @Column(name = "modify_datetime")
    @Schema(description = "수정 일시")
    private LocalDateTime modifyDatetime;

    @CreatedBy
    private transient User createdByUser; // ✅ User 객체를 임시 저장 (DB 에는 저장되지 않음)

    @LastModifiedBy
    private transient User modifiedByUser;

    @Column(name = "creator_id", nullable = false, updatable = false)
    @Schema(description = "생성자 ID")
    private String creatorId = "anonymous";

    @Column(name = "creator_login_id", nullable = false, updatable = false)
    @Schema(description = "생성자 로그인 ID")
    private String creatorLoginId = "anonymous";

    @Column(name = "creator_name", nullable = false, updatable = false)
    @Schema(description = "생성자 명")
    private String creatorName = "anonymous";

    @Column(name = "modifier_id")
    @Schema(description = "수정자 ID")
    private String modifierId;

    @Column(name = "modifier_login_id")
    @Schema(description = "수정자 로그인 ID")
    private String modifierLoginId;

    @Column(name = "modifier_name")
    @Schema(description = "수정자 명")
    private String modifierName;

    // ✅ `modify_datetime`가 생성 시에는 `null`이 유지되도록 설정
    @PrePersist
    public void prePersist() {
        this.modifyDatetime = null; // ❌ 생성 시에는 null 로 설정
    }

    @PreUpdate
    public void preUpdate() {
        this.modifyDatetime = LocalDateTime.now(); // ✅ 수정될 때만 현재 시간으로 설정
    }

    // ✅ JPA가 자동으로 이 메서드를 호출하여 값 매핑
    public void setCreatedByUser(User user) {
        if (user != null) {
            this.creatorId = user.getUserId();
            this.creatorLoginId = user.getLoginId();
            this.creatorName = user.getUserName();
        }
    }

    public void setModifiedByUser(User user) {
        if (user != null) {
            this.modifierId = user.getUserId();
            this.modifierLoginId = user.getLoginId();
            this.modifierName = user.getUserName();
        }
    }
}
