package com.bb.ballBin.board.repository;

import com.bb.ballBin.board.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface BoardRepository extends JpaRepository<Board, String> {
    Page<Board> findByBoardType(String boardType, Pageable pageable);
}
