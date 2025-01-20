package com.bb.ballBin.board.controller;

import com.bb.ballBin.board.dto.BoardRequestDto;
import com.bb.ballBin.board.dto.BoardResponseDto;
import com.bb.ballBin.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/board")
public class BoardController {

    private final BoardService boardService;

    // 모든 게시글 조회
    @GetMapping
    public ResponseEntity<List<BoardResponseDto>> getAllBoards() {
        return ResponseEntity.ok(boardService.getAllBoards());
    }

    // 게시글 생성
    @PostMapping
    public ResponseEntity<BoardResponseDto> createBoard(@RequestBody BoardRequestDto requestDto) {
        return ResponseEntity.ok(boardService.createBoard(requestDto));
    }

    // 게시글 수정
    @PutMapping("/{id}")
    public ResponseEntity<BoardResponseDto> updateBoard(
            @PathVariable UUID id,
            @RequestBody BoardRequestDto requestDto
    ) {
        return ResponseEntity.ok(boardService.updateBoard(id, requestDto));
    }
}
