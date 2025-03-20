package com.bb.ballBin.board.controller;

import com.bb.ballBin.board.entity.BoardType;
import com.bb.ballBin.board.model.BoardRequestDto;
import com.bb.ballBin.board.model.BoardResponseDto;
import com.bb.ballBin.board.model.BoardViewRequestDto;
import com.bb.ballBin.board.service.BoardService;
import com.bb.ballBin.common.message.annotation.MessageKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    public ResponseEntity<Page<BoardResponseDto>> boardList(
            @RequestParam("boardType") BoardType boardType,
            @PageableDefault(page = 0, size = 8, sort = "createDatetime", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(boardService.getAllBoards(boardType, pageable));
    }


    @GetMapping("/{boardId}")
    @Operation(summary = "게시글 개별 조회")
    public ResponseEntity<BoardResponseDto> boardDetail(
            @PathVariable("boardId") String boardId) {

        BoardResponseDto board = boardService.getBoardById(boardId);
        return ResponseEntity.ok(board);
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
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        boardService.createBoard(boardRequestDto, files);

        return ResponseEntity.ok().build();
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
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        boardService.updateBoard(boardId, boardRequestDto, files);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/views")
    @Operation(summary = "게시글 조회 증가")
    @MessageKey(value = "success.update")
    public ResponseEntity<String> viewBoards(@RequestBody BoardViewRequestDto boardViewRequestDto){

        boardService.viewUpdateBoard(boardViewRequestDto);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{boardId}")
    @Operation(summary = "게시글 삭제")
    @MessageKey(value = "success.board.delete")
    public ResponseEntity<String> deleteBoard(@PathVariable("boardId") String boardId) {

        boardService.deleteBoardAndFile(boardId);

        return ResponseEntity.ok().build();
    }
}
