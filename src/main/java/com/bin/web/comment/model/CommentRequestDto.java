package com.bin.web.comment.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentRequestDto {

    @Schema(description = "댓글을 작성할 게시글 ID")
    private String boardId;

    @Schema(description = "부모 댓글 ID")
    private String parentId;

    @Schema(description = "댓글 내용")
    private String content;
}
