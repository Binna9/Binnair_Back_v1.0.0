package com.bb.ballBin.board.entity;

import com.bb.ballBin.board.domain.BoardType;
import com.bb.ballBin.common.entity.BaseEntity;
import com.bb.ballBin.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "boards")
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(updatable = false, nullable = false, unique = true, name = "board_id")
    private String boardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", length = 20, nullable = false)
    private BoardType boardType;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "views", nullable = false)
    private Integer views = 0;

    @Column(name = "likes", nullable = false)
    private Integer likes = 0;

    @Column(name = "file_path")
    private String filePath;

    @ManyToOne(fetch = FetchType.LAZY) // ✅ N:1 관계 설정 (지연 로딩 적용)
    @JoinColumn(name = "writer_id", referencedColumnName = "user_id", nullable = false)
    private User writer;

    @Column(name = "writer_name", length = 30, nullable = false)
    private String writerName;
}
