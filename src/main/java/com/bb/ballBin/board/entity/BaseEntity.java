package com.bb.ballBin.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass // 공통 필드를 상속받는 엔티티에 매핑
@EntityListeners(AuditingEntityListener.class) // Auditing 기능 활성화
public abstract class BaseEntity {

    @CreatedDate // 엔티티가 생성될 때 자동으로 값이 저장됨
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate // 엔티티가 수정될 때 자동으로 값이 갱신됨
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
