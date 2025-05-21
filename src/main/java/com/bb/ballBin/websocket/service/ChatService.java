package com.bb.ballBin.websocket.service;

import com.bb.ballBin.websocket.entity.Chat;
import com.bb.ballBin.websocket.model.ChatMessageResponseDto;
import com.bb.ballBin.websocket.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    public ResponseEntity<List<ChatMessageResponseDto>> getChatHistoryList (){

        List<Chat> chatList = chatRepository.findTop50ByOrderByTimestampDesc();

        List<ChatMessageResponseDto> dtoList = chatList.stream()
                .map(chat -> new ChatMessageResponseDto(
                        chat.getSender(),
                        chat.getContent(),
                        chat.getTimestamp().toString()
                ))
                .sorted(Comparator.comparing(ChatMessageResponseDto::getTimestamp))
                .toList();

        return ResponseEntity.ok(dtoList);
    }
}
