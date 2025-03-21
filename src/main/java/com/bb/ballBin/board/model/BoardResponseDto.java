package com.bb.ballBin.board.model;

import com.bb.ballBin.board.entity.BoardType;
import com.bb.ballBin.board.entity.Board;
import com.bb.ballBin.comment.model.CommentResponseDto;
import com.bb.ballBin.file.entity.File;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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
    private int views;
    @Schema(name = "게시글 좋아요 수")
    private int likes;
    @Schema(name = "게시글 싫어요 수")
    private int unlikes;

    @Schema(name = "게시글 작성자 ID")
    private String writerId;
    @Schema(name = "게시글 작성자 명")
    private String writerName;

    @Schema(name = "생성 일자")
    private LocalDateTime createDatetime;
    @Schema(name = "수정 일자")
    private LocalDateTime modifyDatetime;

    @Schema(name = "게시판 댓글")
    private List<CommentResponseDto> comments;

    @Schema(name = "게시판 파일")
    private List<File> files;
}
