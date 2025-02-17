package com.bb.ballBin.bookmark.controller;

import com.bb.ballBin.bookmark.model.BookmarkRequestDto;
import com.bb.ballBin.bookmark.model.BookmarkResponseDto;
import com.bb.ballBin.bookmark.service.BookmarkService;
import com.bb.ballBin.common.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    /**
     * 특정 사용자의 즐겨찾기 목록 조회
     */
    @GetMapping("")
    @Operation(summary = "현재 로그인한 사용자의 즐겨찾기 목록 조회")
    public ResponseEntity<List<BookmarkResponseDto>> getUserBookmarks() {
        String userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(bookmarkService.getUserBookmarks(userId));
    }

    /**
     * 즐겨찾기 추가 (로그인한 사용자 정보 자동 적용)
     */
    @PostMapping("")
    @Operation(summary = "즐겨찾기 추가")
    public ResponseEntity<BookmarkResponseDto> addBookmark(@RequestBody BookmarkRequestDto bookmarkRequestDto) {
        String userId = SecurityUtil.getCurrentUserId(); // ✅ 로그인한 사용자의 ID 가져오기
        return ResponseEntity.status(HttpStatus.CREATED).body(bookmarkService.addBookmark(userId, bookmarkRequestDto));
    }

    /**
     * 즐겨찾기 삭제
     */
    @DeleteMapping("/{bookmarkId}")
    @Operation(summary = "즐겨찾기 삭제")
    public ResponseEntity<Void> removeBookmark(@PathVariable String bookmarkId) {
        bookmarkService.removeBookmark(bookmarkId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
