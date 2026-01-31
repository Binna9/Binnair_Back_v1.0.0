package com.bin.web.comment.controller;

import com.bin.web.comment.model.CommentRequestDto;
import com.bin.web.comment.model.CommentUpdateRequestDto;
import com.bin.web.comment.service.CommentService;
import com.bin.web.common.annotation.MessageKey;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("")
    @PreAuthorize("hasAuthority('COMMENT_CREATE')")
    @Operation(description = "댓글 생성")
    @MessageKey(value = "success.comment.create")
    public ResponseEntity<Void> createComment(@RequestBody CommentRequestDto commentRequestDto) {
        commentService.createComment(commentRequestDto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("hasAuthority('COMMENT_UPDATE')")
    @Operation(description = "댓글 수정")
    @MessageKey(value = "success.comment.update")
    public ResponseEntity<Void> updateComment(@PathVariable String commentId, @RequestBody CommentUpdateRequestDto commentUpdateRequestDto) {
        commentService.updateComment(commentId, commentUpdateRequestDto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasAuthority('COMMENT_DELETE')")
    @Operation(description = "댓글 삭제")
    @MessageKey(value = "success.comment.delete")
    public ResponseEntity<Void> deleteComment(@PathVariable String commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok().build();
    }
}
