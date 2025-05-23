package com.bb.ballBin.board.controller;

import com.bb.ballBin.board.entity.BoardType;
import com.bb.ballBin.board.model.BoardRequestDto;
import com.bb.ballBin.board.model.BoardResponseDto;
import com.bb.ballBin.board.model.BoardViewRequestDto;
import com.bb.ballBin.board.service.BoardService;
import com.bb.ballBin.common.annotation.MessageKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/boards")
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    @PreAuthorize("hasAuthority('BOARD_READ')")
    @Operation(summary = "게시글 전체 조회")
    public ResponseEntity<Page<BoardResponseDto>> getAllBoards(
            @RequestParam("boardType") BoardType boardType,
            @PageableDefault(page = 0, size = 8, sort = "createDatetime", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(boardService.allBoards(boardType, pageable));
    }

    @GetMapping("/{boardId}")
    @PreAuthorize("hasAuthority('BOARD_READ')")
    @Operation(summary = "게시글 개별 조회")
    public ResponseEntity<BoardResponseDto> getBoardById(@PathVariable("boardId") String boardId) {
        return ResponseEntity.ok(boardService.boardById(boardId));
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority('BOARD_CREATE')")
    @MessageKey(value = "success.board.create")
    @Operation(summary = "게시글 생성",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(implementation = BoardRequestDto.class)
                    )
            ))
    public ResponseEntity<Void> createBoard(@ModelAttribute BoardRequestDto boardRequestDto,
                                            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        boardService.addBoard(boardRequestDto, files);

        return ResponseEntity.ok().build();
    }


    @PutMapping("/{boardId}")
    @PreAuthorize("hasAuthority('BOARD_UPDATE')")
    @Operation(summary = "게시글 수정")
    @MessageKey(value = "success.board.update")
    public ResponseEntity<Void> modifyBoard(@PathVariable("boardId") String boardId, @ModelAttribute BoardRequestDto boardRequestDto) {

        boardService.updateBoard(boardId, boardRequestDto);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{boardId}")
    @PreAuthorize("hasAuthority('BOARD_DELETE')")
    @Operation(summary = "게시글 삭제")
    @MessageKey(value = "success.board.delete")
    public ResponseEntity<Void> removeBoard(@PathVariable("boardId") String boardId) {

        boardService.deleteBoard(boardId);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/views")
    @Operation(summary = "게시글 조회 증가")
    public ResponseEntity<Void> viewBoards(@RequestBody BoardViewRequestDto boardViewRequestDto) {

        boardService.viewUpdateBoard(boardViewRequestDto);

        return ResponseEntity.ok().build();
    }
}
