package com.bb.ballBin.like.controller;

import com.bb.ballBin.common.util.SecurityUtil;
import com.bb.ballBin.like.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/{boardId}/like")
    @PreAuthorize("hasAuthority('LIKE')")
    @Operation(summary = "사용자 , 게시판 별 좋아요")
    public ResponseEntity<String> toggleLike(@PathVariable String boardId) {

        String userId = SecurityUtil.getCurrentUserId();
        likeService.toggleLike(userId, boardId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{boardId}/unlike")
    @PreAuthorize("hasAuthority('UNLIKE')")
    @Operation(summary = "사용자 , 게시판 별 싫어요")
    public ResponseEntity<String> toggleUnlike(@PathVariable String boardId) {

        String userId = SecurityUtil.getCurrentUserId();
        likeService.toggleUnlike(userId, boardId);

        return ResponseEntity.ok().build();
    }
}

