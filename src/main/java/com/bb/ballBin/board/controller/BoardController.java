package com.bb.ballBin.board.controller;

import com.bb.ballBin.board.model.BoardRequestDto;
import com.bb.ballBin.board.model.BoardResponseDto;
import com.bb.ballBin.board.service.BoardService;
import com.bb.ballBin.common.message.annotation.MessageKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/boards")
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    @Operation(summary = "게시글 전체 조회")
    public ResponseEntity<List<BoardResponseDto>> boardList(
            @RequestParam String boardType,
            @PageableDefault(size = 10, sort = "createDatetime", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(boardService.getAllBoards(boardType, pageable));
    }

    @GetMapping("/{boardId}")
    @Operation(summary = "게시글 개별 조회")
    public ResponseEntity<BoardResponseDto> boardDetail(@PathVariable("boardId") String boardId) {

        return ResponseEntity.ok(boardService.getBoardById(boardId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "게시글 생성",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(implementation = BoardRequestDto.class)
                    )
            ))
    public ResponseEntity<String> createBoard(
            @ModelAttribute BoardRequestDto boardRequestDto,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        boardService.createBoard(boardRequestDto, file);
        return ResponseEntity.status(HttpStatus.OK).build();
    }


    @PutMapping(value = "/{boardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "게시글 수정",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(implementation = BoardRequestDto.class)
                    )
            ))
    public ResponseEntity<String> updateBoard(
            @PathVariable("boardId") String boardId,
            @ModelAttribute BoardRequestDto boardRequestDto,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        boardService.updateBoard(boardId, boardRequestDto, file);
        return ResponseEntity.status(HttpStatus.OK).build();
    }


    @DeleteMapping("/{boardId}")
    @Operation(summary = "게시글 삭제")
    @MessageKey(value = "success.board.delete")
    public ResponseEntity<Void> deleteBoard(@PathVariable("boardId") String boardId) {

        boardService.deleteBoard(boardId);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
