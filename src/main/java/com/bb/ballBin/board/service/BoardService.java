package com.bb.ballBin.board.service;

import com.bb.ballBin.board.entity.BoardType;
import com.bb.ballBin.board.entity.Board;
import com.bb.ballBin.board.mapper.BoardMapper;
import com.bb.ballBin.board.model.BoardRequestDto;
import com.bb.ballBin.board.model.BoardResponseDto;
import com.bb.ballBin.board.model.BoardViewRequestDto;
import com.bb.ballBin.board.repository.BoardRepository;
import com.bb.ballBin.comment.model.CommentResponseDto;
import com.bb.ballBin.comment.repository.CommentRepository;
import com.bb.ballBin.common.exception.NotFoundException;
import com.bb.ballBin.common.util.SecurityUtil;
import com.bb.ballBin.file.entity.File;
import com.bb.ballBin.file.entity.TargetType;
import com.bb.ballBin.file.service.FileService;
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

    private final BoardMapper boardMapper;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final FileService fileService;

    /**
     * 게시글 목록 조회
     */
    public Page<BoardResponseDto> allBoards(BoardType boardType, Pageable pageable) {
        return boardRepository.findByBoardType(boardType, pageable)
                .map(boardMapper::toDto);
    }

    /**
     * 개별 게시글 조회
     */
    @Transactional
    public BoardResponseDto boardById(String boardId) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundException("error.board.notfound"));

        List<CommentResponseDto> comments = commentRepository.findByBoard_BoardIdAndParentIsNull(boardId)
                .stream()
                .map(CommentResponseDto::from)
                .toList();

        List<File> files = fileService.getFilesByTarget(TargetType.BOARD, boardId);

        BoardResponseDto responseDto = boardMapper.toDto(board);
        responseDto.setComments(comments);
        responseDto.setFiles(files);

        return responseDto;
    }

    @Transactional(readOnly = true)
    public int getCommentCountByBoardId(String boardId) {

        boardRepository.findById(boardId).orElseThrow(() -> new NotFoundException("error.board.notfound"));

        return commentRepository.countByBoard_BoardIdAndParentIsNull(boardId);
    }

    /**
     * 게시글 생성
     */
    @Transactional
    public void addBoard(BoardRequestDto boardRequestDto, List<MultipartFile> files) {
        try {
            if (boardRequestDto.getBoardType() == null) {
                throw new IllegalArgumentException("error.board.type");
            }

            if (!BoardType.isValidType(boardRequestDto.getBoardType().name())) {
                throw new IllegalArgumentException("error.board.not_type");
            }

            String userId = SecurityUtil.getCurrentUserId();
            User writer = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("error.user.notfound"));

            Board board = boardMapper.toEntity(boardRequestDto);
            board.setWriter(writer);
            board.setWriterName(writer.getNickName());

            board = boardRepository.save(board);

            if (files != null && !files.isEmpty()) {
                fileService.uploadFiles(TargetType.BOARD, board.getBoardId(), files);
            }

        } catch (Exception e) {
            throw new RuntimeException("error.runtime", e);
        }
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public void updateBoard(String boardId, BoardRequestDto boardRequestDto) {
        try {
            Board board = boardRepository.findById(boardId)
                    .orElseThrow(() -> new NotFoundException("error.board.notfound"));

            boardMapper.updateEntity(boardRequestDto, board);

            boardRepository.save(board);

        } catch (Exception e) {
            throw new RuntimeException("error.runtime", e);
        }
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void deleteBoard(String boardId) {
        try {
            boardRepository.deleteById(boardId);
            fileService.deleteFilesByTarget(TargetType.BOARD, boardId);
        } catch (Exception e) {
            throw new RuntimeException("error.runtime", e);
        }
    }

    /**
     * 게시글 조회 수 증가
     */
    public void viewUpdateBoard(BoardViewRequestDto boardViewRequestDto) {

        Board board = boardRepository.findById(boardViewRequestDto.getBoardId())
                .orElseThrow(() -> new NotFoundException("error.board.notfound"));

        board.setViews(boardViewRequestDto.getViews());

        boardRepository.save(board);
    }
}
