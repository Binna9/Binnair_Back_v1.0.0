package com.bb.ballBin.board.model;

import com.bb.ballBin.board.domain.BoardType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardResponseDto {

    @Schema(name = "게시글 ID")
    private String boardId;
    @Schema(name = "게시글 종류 타입")
    private BoardType boardType;
    @Schema(name = "게시글 제목")
    private String title;
    @Schema(name = "게시글 내용")
    private String content;
    @Schema(name = "게시글 조회 수")
    private Integer views;
    @Schema(name = "게시글 좋아요 수")
    private Integer likes;
    @Schema(name = "게시글 작성자 ID")
    private String writerId;
    @Schema(name = "게시글 작성자 명")
    private String writerName;
    @Schema(name = "게시글 첨부 파일 주소")
    private String filePath;
    @Schema(name = "생성 일자")
    private LocalDateTime createDatetime;
    @Schema(name = "수정 일자")
    private LocalDateTime modifyDatetime;
}
