package com.bb.ballBin.bookmark.controller;

import com.bb.ballBin.bookmark.model.BookmarkRequestDto;
import com.bb.ballBin.bookmark.model.BookmarkResponseDto;
import com.bb.ballBin.bookmark.service.BookmarkService;
import com.bb.ballBin.common.annotation.MessageKey;
import com.bb.ballBin.common.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping("")
    @Operation(summary = "즐겨찾기 목록 조회")
    public ResponseEntity<Page<BookmarkResponseDto>> getAllBookmarks(
            @PageableDefault(page = 0, size = 9, sort = "createDatetime", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(bookmarkService.allBookmarks(userId, pageable));
    }

    @PostMapping("")
    @Operation(summary = "즐겨찾기 추가")
    @MessageKey(value = "success.bookmark.create")
    public ResponseEntity<BookmarkResponseDto> createBookmark(@RequestBody BookmarkRequestDto bookmarkRequestDto) {

        String userId = SecurityUtil.getCurrentUserId();

        bookmarkService.addBookmark(userId, bookmarkRequestDto);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{bookmarkId}")
    @Operation(summary = "즐겨찾기 삭제")
    @MessageKey(value = "success.bookmark.delete")
    public ResponseEntity<Void> removeBookmark(@PathVariable String bookmarkId) {

        bookmarkService.removeBookmark(bookmarkId);

        return ResponseEntity.ok().build();
    }
}
