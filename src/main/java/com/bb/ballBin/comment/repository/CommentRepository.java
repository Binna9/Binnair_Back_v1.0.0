package com.bb.ballBin.comment.repository;

import com.bb.ballBin.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {

    List<Comment> findByBoard_BoardIdAndParentIsNull(String boardId);

    int countByBoard_BoardIdAndParentIsNull(String boardId);
}
