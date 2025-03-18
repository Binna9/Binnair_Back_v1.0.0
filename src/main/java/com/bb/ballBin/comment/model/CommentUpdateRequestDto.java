package com.bb.ballBin.comment.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentUpdateRequestDto {

    @Schema(description = "댓글 내용")
    private String content;
}
