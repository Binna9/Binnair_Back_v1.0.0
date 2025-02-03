package com.bb.ballBin.board.service;

import com.bb.ballBin.board.model.BoardRequestDto;
import com.bb.ballBin.board.model.BoardResponseDto;
import com.bb.ballBin.board.entity.Board;
import com.bb.ballBin.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BoardService {

    private final BoardRepository boardRepository;

    // 모든 게시글 조회
    public List<BoardResponseDto> getAllBoards() {
        return boardRepository.findAll().stream()
                .map(board -> {
                    BoardResponseDto responseDto = new BoardResponseDto();
                    responseDto.setId(board.getId());
                    responseDto.setTitle(board.getTitle());
                    responseDto.setContent(board.getContent());
                    responseDto.setAuthor(board.getAuthor());
                    responseDto.setCreatedAt(board.getCreatedAt());
                    responseDto.setUpdatedAt(board.getUpdatedAt());
                    return responseDto;
                })
                .collect(Collectors.toList());
    }

    // 게시글 생성
    public BoardResponseDto createBoard(BoardRequestDto requestDto) {
        Board board = new Board();
        board.setTitle(requestDto.getTitle());
        board.setContent(requestDto.getContent());
        board.setAuthor(requestDto.getAuthor());
        Board savedBoard = boardRepository.save(board);

        return toResponseDto(savedBoard);
    }

    // 게시글 수정
    public BoardResponseDto updateBoard(UUID id, BoardRequestDto requestDto) {
        Optional<Board> optionalBoard = boardRepository.findById(id);
        if (optionalBoard.isEmpty()) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다. ID: " + id);
        }

        Board board = optionalBoard.get();
        board.setTitle(requestDto.getTitle());
        board.setContent(requestDto.getContent());
        board.setAuthor(requestDto.getAuthor()); // 작성자도 수정 가능
        Board updatedBoard = boardRepository.save(board);

        return toResponseDto(updatedBoard);
    }

    // 응답 DTO로 변환하는 메서드
    private BoardResponseDto toResponseDto(Board board) {
        BoardResponseDto responseDto = new BoardResponseDto();
        responseDto.setId(board.getId());
        responseDto.setTitle(board.getTitle());
        responseDto.setContent(board.getContent());
        responseDto.setAuthor(board.getAuthor());
        responseDto.setCreatedAt(board.getCreatedAt());
        responseDto.setUpdatedAt(board.getUpdatedAt());
        return responseDto;
    }
}
