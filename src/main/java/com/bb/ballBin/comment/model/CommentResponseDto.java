package com.bb.ballBin.comment.model;

import com.bb.ballBin.comment.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDto {

    @Schema(name = "댓글 ID")
    private String commentId;
    @Schema(name = "댓글 내용")
    private String content;
    @Schema(name = "댓글 작성자 ID")
    private String writerId;
    @Schema(name = "댓글 작성자")
    private String writerName;
    @Schema(name = "댓글 작성 일자")
    private LocalDateTime createDatetime;

    @Schema(name = "대댓글 리스트")
    private List<CommentResponseDto> replies;

    /**
     * ✅ `Comment` 엔티티 → `CommentResponseDto` 변환
     */
    public static CommentResponseDto from(Comment comment) {
        return CommentResponseDto.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .writerId(comment.getWriter().getUserId())
                .writerName(comment.getWriterName())
                .createDatetime(comment.getCreateDatetime())
                .replies(comment.getReplies().stream()
                        .map(CommentResponseDto::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
