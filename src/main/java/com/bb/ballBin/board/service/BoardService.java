package com.bb.ballBin.board.service;

import com.bb.ballBin.board.domain.BoardType;
import com.bb.ballBin.board.entity.Board;
import com.bb.ballBin.board.model.BoardRequestDto;
import com.bb.ballBin.board.model.BoardResponseDto;
import com.bb.ballBin.board.repository.BoardRepository;
import com.bb.ballBin.common.util.FileUtil;
import com.bb.ballBin.common.util.SecurityUtil;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final FileUtil fileUtil;

    /**
     * 게시글 목록 조회
     */
    public Page<BoardResponseDto> getAllBoards(BoardType boardType, Pageable pageable) {
        return boardRepository.findByBoardType(boardType, pageable)
                .map(Board::toDto);
    }

    /**
     * 개별 게시글 조회
     */
    public BoardResponseDto getBoardById(String boardId) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("error.security.notfound"));

        return board.toDto();
    }

    /**
     * 게시글 생성
     */
    @Transactional
    public void createBoard(BoardRequestDto boardRequestDto, MultipartFile file) {
        try {
            if (boardRequestDto.getBoardType() == null) {
                throw new IllegalArgumentException("❌ 게시판 유형(boardType)은 필수입니다.");
            }

            // ✅ 유효한 boardType 인지 검증
            if (!BoardType.isValidType(boardRequestDto.getBoardType().name())) {
                throw new IllegalArgumentException("❌ 유효하지 않은 게시판 유형입니다: " + boardRequestDto.getBoardType());
            }

            String userId = SecurityUtil.getCurrentUserId();
            User writer = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("error.user.notfound"));

            Board board = Board.builder()
                    .boardType(boardRequestDto.getBoardType())
                    .title(boardRequestDto.getTitle())
                    .content(boardRequestDto.getContent())
                    .writer(writer)
                    .writerName(writer.getUserName())
                    .build();

            board = boardRepository.save(board);

            if (file != null && !file.isEmpty()) {
                String filePath = fileUtil.saveFile("board" , board.getBoardId(), file);
                board.setFilePath(filePath);
                boardRepository.save(board);
            }

        } catch (Exception e) {
            System.err.println("🔴 JPA 예외 발생: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("게시글 저장 중 오류 발생", e);
        }
    }

    /**
     * 게시글 수정
     */
    public void updateBoard(String boardId, BoardRequestDto boardRequestDto, MultipartFile file) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("error.board.notfound"));

        board.setTitle(boardRequestDto.getTitle());
        board.setContent(boardRequestDto.getContent());

        if (file != null && !file.isEmpty()) {
            String filePath = fileUtil.saveFile(null, board.getBoardId(), file);
            board.setFilePath(filePath);
        }

        boardRepository.save(board);
    }

    /**
     * 게시글 삭제
     */
    public void deleteBoard(String boardId) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("error.board.notfound"));

        boardRepository.delete(board);
    }
}
