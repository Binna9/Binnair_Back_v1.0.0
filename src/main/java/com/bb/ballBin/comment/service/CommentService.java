package com.bb.ballBin.comment.service;

import com.bb.ballBin.board.entity.Board;
import com.bb.ballBin.board.repository.BoardRepository;
import com.bb.ballBin.comment.entity.Comment;
import com.bb.ballBin.comment.model.CommentRequestDto;
import com.bb.ballBin.comment.model.CommentUpdateRequestDto;
import com.bb.ballBin.comment.repository.CommentRepository;
import com.bb.ballBin.common.exception.NotFoundException;
import com.bb.ballBin.common.util.SecurityUtil;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    /**
     * 댓글 생성 (대댓글 포함)
     */
    @Transactional
    public void createComment(CommentRequestDto commentRequestDto) {
        try {
            User writer = userRepository.findById(SecurityUtil.getCurrentUserId())
                    .orElseThrow(() -> new NotFoundException("error.user.notfound"));

            Board board = boardRepository.findById(commentRequestDto.getBoardId())
                    .orElseThrow(() -> new NotFoundException("error.board.notfound"));

            Comment parentComment = null;

            if (commentRequestDto.getParentId() != null && !commentRequestDto.getParentId().trim().isEmpty()) {
                parentComment = commentRepository.findById(commentRequestDto.getParentId())
                        .orElse(null);

                if (parentComment == null) {
                    throw new NotFoundException("error.comment.notfound");
                }
            }

            Comment comment = Comment.builder()
                    .board(board)
                    .parent(parentComment)
                    .writer(writer)
                    .writerName(writer.getNickName())
                    .content(commentRequestDto.getContent())
                    .build();

            commentRepository.save(comment);

        } catch(Exception e){
            logger.error(e.getMessage());
            throw new RuntimeException("처리중 오류 발생", e);
        }
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public void updateComment(String commentId, CommentUpdateRequestDto commentUpdateRequestDto) {
        try {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new NotFoundException("error.comment.notfound"));

            if (!SecurityUtil.getCurrentUserId().equals(comment.getWriter().getUserId())) {
                throw new AccessDeniedException("error.comment.access");
            }

            comment.setContent(commentUpdateRequestDto.getContent());

            commentRepository.save(comment);

        } catch (Exception e) {
            logger.error("오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("처리 중 오류 발생", e);
        }
    }

    /**
     * 댓글 삭제 (대댓글 포함)
     */
    @Transactional
    public void deleteComment(String commentId) {
        try {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new RuntimeException("error.comment.notfound"));

            if (!SecurityUtil.getCurrentUserId().equals(comment.getWriter().getUserId())) {
                throw new AccessDeniedException("error.comment.access");
            }

            commentRepository.delete(comment);

        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException("삭제 중 오류 발생", e);
        }
    }
}
