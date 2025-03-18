package com.bb.ballBin.board.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardViewRequestDto {

    @Schema(name = "게시글 ID")
    private String boardId;
    @Schema(name = "게시글 조회 수")
    private int views;
}
