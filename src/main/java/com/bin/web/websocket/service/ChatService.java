package com.bin.web.websocket.service;

import com.bin.web.websocket.entity.Chat;
import com.bin.web.websocket.model.ChatMessageResponseDto;
import com.bin.web.websocket.repository.ChatRepository;
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
