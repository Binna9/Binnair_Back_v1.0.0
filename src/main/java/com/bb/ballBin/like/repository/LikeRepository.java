package com.bb.ballBin.like.repository;

import com.bb.ballBin.board.entity.Board;
import com.bb.ballBin.like.entity.Like;
import com.bb.ballBin.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, String> {

    Optional<Like> findByUserAndBoard(User user, Board board);
}
