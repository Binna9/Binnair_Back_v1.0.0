package com.bb.ballBin.board.controller;

import com.bb.ballBin.board.model.BoardRequestDto;
import com.bb.ballBin.board.model.BoardResponseDto;
import com.bb.ballBin.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/boards")
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public ResponseEntity<List<BoardResponseDto>> getAllBoards() {
        return ResponseEntity.ok(boardService.getAllBoards());
    }

    @PostMapping
    public ResponseEntity<BoardResponseDto> createBoard(@RequestBody BoardRequestDto requestDto) {
        return ResponseEntity.ok(boardService.createBoard(requestDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BoardResponseDto> updateBoard(
            @PathVariable UUID id,
            @RequestBody BoardRequestDto requestDto
    ) {
        return ResponseEntity.ok(boardService.updateBoard(id, requestDto));
    }
}
