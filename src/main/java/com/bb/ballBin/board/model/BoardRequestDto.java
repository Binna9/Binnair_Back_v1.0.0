package com.bb.ballBin.board.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardRequestDto {

    @Schema(name = "게시글 종류 타입")
    private String boardType; // 공지사항, 커뮤니티, 1:1 문의, 자주하는 질문
    @Schema(name = "게시글 제목")
    private String title;
    @Schema(name = "게시글 내용")
    private String content;
}
