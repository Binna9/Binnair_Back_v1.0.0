package com.bb.ballBin.common.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(BaseEntityListener.class)
@MappedSuperclass
public class BaseEntity {

    @CreatedDate
    @Column(name = "create_datetime", nullable = false, updatable = false)
    @Schema(description = "생성 일시")
    private Timestamp createDatetime;

    @CreatedBy
    @Column(name = "creator_id", nullable = false, updatable = false)
    @Schema(description = "생성자 ID")
    private String creatorId;

    @CreatedBy
    @Column(name = "creator_login_id", nullable = false, updatable = false)
    @Schema(description = "생성자 로그인 ID")
    private String creatorLoginId;

    @CreatedBy
    @Column(name = "creator_name", nullable = false, updatable = false)
    @Schema(description = "생성자 명")
    private String creatorName;

    @LastModifiedDate
    @Column(name = "modify_datetime", nullable = false)
    @Schema(description = "수정 일시")
    private Timestamp modifyDatetime;

    @LastModifiedBy
    @Column(name = "modifier_id", nullable = false)
    @Schema(description = "수정자 ID")
    private String modifierId;

    @LastModifiedBy
    @Column(name = "modifier_login_id", nullable = false)
    @Schema(description = "수정자 로그인 ID")
    private String modifierLoginId;

    @LastModifiedBy
    @Column(name = "modifier_name", nullable = false)
    @Schema(description = "수정자 명")
    private String modifierName;
}
