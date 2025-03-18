package com.bb.ballBin.board.service;

import com.bb.ballBin.board.domain.BoardType;
import com.bb.ballBin.board.entity.Board;
import com.bb.ballBin.board.model.BoardRequestDto;
import com.bb.ballBin.board.model.BoardResponseDto;
import com.bb.ballBin.board.repository.BoardRepository;
import com.bb.ballBin.comment.model.CommentResponseDto;
import com.bb.ballBin.comment.repository.CommentRepository;
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

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final FileUtil fileUtil;

    /**
     * 게시글 목록 조회
     */
    public Page<BoardResponseDto> getAllBoards(BoardType boardType, Pageable pageable) {
        return boardRepository.findByBoardType(boardType, pageable)
                .map(board -> BoardResponseDto.from(board, List.of()));
    }

    /**
     * 개별 게시글 조회
     */
    @Transactional
    public BoardResponseDto getBoardById(String boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("❌ 해당 게시글을 찾을 수 없습니다."));

        // ✅ 댓글 조회 (부모 댓글만 가져오기)
        List<CommentResponseDto> comments = commentRepository.findByBoard_BoardIdAndParentIsNull(boardId)
                .stream()
                .map(CommentResponseDto::from)
                .toList();

        return BoardResponseDto.from(board, comments); // ✅ 댓글 포함하여 DTO 변환
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
                    .writerName(writer.getNickName())
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
    @Transactional
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
