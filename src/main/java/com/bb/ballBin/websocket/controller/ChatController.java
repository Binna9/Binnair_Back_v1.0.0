package com.bb.ballBin.websocket.controller;

import com.bb.ballBin.websocket.model.ChatMessageResponseDto;
import com.bb.ballBin.websocket.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chats")
public class ChatController {

    private final ChatService chatService;

    @GetMapping("")
    @PreAuthorize("hasAuthority('WEBSOCKET_READ')")
    @Operation(summary = "채팅 50개 조회")
    public ResponseEntity<List<ChatMessageResponseDto>> getChatHistory() {
        return chatService.getChatHistoryList();
    }
}
