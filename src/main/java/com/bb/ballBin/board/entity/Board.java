package com.bb.ballBin.board.entity;

import com.bb.ballBin.board.domain.BoardType;
import com.bb.ballBin.board.model.BoardResponseDto;
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

    @Enumerated(EnumType.STRING) // ✅ Enum을 문자열로 저장
    @Column(name = "board_type", length = 20, nullable = false)
    private BoardType boardType; // 공지사항, 커뮤니티, 1:1 문의, 자주하는 질문

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

    /**
     * ✅ Entity → DTO 변환 (인스턴스 메서드)
     */
    public BoardResponseDto toDto() {
        return BoardResponseDto.builder()
                .boardId(this.boardId)
                .boardType(this.boardType)
                .title(this.title)
                .content(this.content)
                .views(this.views)
                .likes(this.likes)
                .filePath(this.filePath)
                .writerId(this.writer.getUserId()) // ✅ writer에서 userId 가져오기
                .writerName(this.writerName)
                .createDatetime(this.getCreateDatetime())
                .modifyDatetime(this.getModifyDatetime())
                .build();
    }
}
