package com.bin.web.board.repository;

import com.bin.web.board.entity.BoardType;
import com.bin.web.board.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface BoardRepository extends JpaRepository<Board, String> {
    Page<Board> findByBoardType(BoardType boardType, Pageable pageable);
}
