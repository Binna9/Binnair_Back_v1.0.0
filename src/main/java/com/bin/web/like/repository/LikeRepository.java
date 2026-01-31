package com.bin.web.like.repository;

import com.bin.web.board.entity.Board;
import com.bin.web.like.entity.Like;
import com.bin.web.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, String> {

    Optional<Like> findByUserAndBoard(User user, Board board);
}
