package com.bb.ballBin.websocket.repository;

import com.bb.ballBin.websocket.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, String> {
    List<Chat> findTop50ByOrderByTimestampDesc();
}
