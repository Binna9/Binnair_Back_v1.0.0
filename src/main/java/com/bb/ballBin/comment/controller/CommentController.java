package com.bb.ballBin.comment.controller;

import com.bb.ballBin.comment.model.CommentRequestDto;
import com.bb.ballBin.comment.model.CommentUpdateRequestDto;
import com.bb.ballBin.comment.service.CommentService;
import com.bb.ballBin.common.message.annotation.MessageKey;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("")
    @Operation(description = "댓글 생성")
    @MessageKey(value = "success.comment.create")
    public ResponseEntity<String> createComment(@RequestBody CommentRequestDto commentRequestDto) {
        commentService.createComment(commentRequestDto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{commentId}")
    @Operation(description = "댓글 수정")
    @MessageKey(value = "success.comment.update")
    public ResponseEntity<String> updateComment(@PathVariable String commentId, @RequestBody CommentUpdateRequestDto commentUpdateRequestDto) {
        commentService.updateComment(commentId, commentUpdateRequestDto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{commentId}")
    @Operation(description = "댓글 삭제")
    @MessageKey(value = "success.comment.delete")
    public ResponseEntity<String> deleteComment(@PathVariable String commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok().build();
    }
}
