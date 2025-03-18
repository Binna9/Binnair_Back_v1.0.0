package com.bb.ballBin.comment.service;

import com.bb.ballBin.board.entity.Board;
import com.bb.ballBin.board.repository.BoardRepository;
import com.bb.ballBin.comment.entity.Comment;
import com.bb.ballBin.comment.model.CommentRequestDto;
import com.bb.ballBin.comment.model.CommentUpdateRequestDto;
import com.bb.ballBin.comment.repository.CommentRepository;
import com.bb.ballBin.common.util.SecurityUtil;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    /**
     * 댓글 생성 (대댓글 포함)
     */
    @Transactional
    public void createComment(CommentRequestDto commentRequestDto) {

        User writer = userRepository.findById(SecurityUtil.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("❌ 사용자 정보를 찾을 수 없습니다."));

        Board board = boardRepository.findById(commentRequestDto.getBoardId())
                .orElseThrow(() -> new RuntimeException("❌ 댓글을 작성할 게시글을 찾을 수 없습니다."));

        Comment parentComment = null;

        if (commentRequestDto.getParentId() != null && !commentRequestDto.getParentId().trim().isEmpty()) {
            parentComment = commentRepository.findById(commentRequestDto.getParentId())
                    .orElse(null);

            if (parentComment == null) {
                throw new RuntimeException("❌ 부모 댓글을 찾을 수 없습니다. (parentId: " + commentRequestDto.getParentId() + ")");
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
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public void updateComment(String commentId, CommentUpdateRequestDto commentUpdateRequestDto) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("❌ 수정할 댓글을 찾을 수 없습니다."));

        if (!SecurityUtil.getCurrentUserId().equals(comment.getWriter().getUserId())) {
            throw new RuntimeException("❌ 댓글 수정 권한이 없습니다.");
        }

        comment.setContent(commentUpdateRequestDto.getContent());
    }

    /**
     * 댓글 삭제 (대댓글 포함)
     */
    @Transactional
    public void deleteComment(String commentId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("❌ 삭제할 댓글을 찾을 수 없습니다."));

        if (!SecurityUtil.getCurrentUserId().equals(comment.getWriter().getUserId())) {
            throw new RuntimeException("❌ 댓글 삭제 권한이 없습니다.");
        }

        commentRepository.delete(comment);
    }
}
