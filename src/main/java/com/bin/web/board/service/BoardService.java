package com.bin.web.board.service;

import com.bin.web.board.entity.BoardType;
import com.bin.web.board.entity.Board;
import com.bin.web.board.mapper.BoardMapper;
import com.bin.web.board.model.BoardRequestDto;
import com.bin.web.board.model.BoardResponseDto;
import com.bin.web.board.model.BoardViewRequestDto;
import com.bin.web.board.repository.BoardRepository;
import com.bin.web.comment.model.CommentResponseDto;
import com.bin.web.comment.repository.CommentRepository;
import com.bin.web.common.exception.NotFoundException;
import com.bin.web.common.util.SecurityUtil;
import com.bin.web.file.entity.File;
import com.bin.web.file.entity.TargetType;
import com.bin.web.file.repository.FileRepository;
import com.bin.web.file.service.FileService;
import com.bin.web.user.entity.User;
import com.bin.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardMapper boardMapper;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FileService fileService;

    /**
     * 게시글 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<BoardResponseDto> allBoards(BoardType boardType, Pageable pageable) {

        Page<Board> page = boardRepository.findByBoardType(boardType, pageable);

        List<String> boardIds = page.getContent().stream()
                .map(Board::getBoardId)
                .toList();

        List<File> files =
                fileRepository.findByTargetTypeAndTargetIdIn(TargetType.BOARD, boardIds);

        Map<String, List<File>> filesByBoardId =
                files.stream().collect(Collectors.groupingBy(File::getTargetId));

        return page.map(b -> {
            BoardResponseDto dto = boardMapper.toDto(b);
            dto.setFiles(filesByBoardId.getOrDefault(b.getBoardId(), List.of()));
            return dto;
        });
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
