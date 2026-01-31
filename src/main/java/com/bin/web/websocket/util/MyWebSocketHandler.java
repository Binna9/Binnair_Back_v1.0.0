package com.bin.web.websocket.util;

import com.bin.web.common.exception.NotFoundException;
import com.bin.web.user.entity.User;
import com.bin.web.user.repository.UserRepository;
import com.bin.web.websocket.entity.Chat;
import com.bin.web.websocket.model.ChatMessageRequestDto;
import com.bin.web.websocket.repository.ChatRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MyWebSocketHandler extends TextWebSocketHandler {

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        String userId = (String) session.getAttributes().get("userId");
        sessions.put(session.getId(), session);

        if (userId != null) {
            System.out.println("✅ WebSocket 연결한 사용자 ID: " + userId);
        } else {
            System.out.println("❌ JWT 없음 또는 유효하지 않음");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        String userId = (String) session.getAttributes().get("userId");
        if (userId == null) throw new NotFoundException("WebSocket 세션에 사용자 정보가 없습니다.");

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다: " + userId));

        ChatMessageRequestDto chatMessageRequestDto = objectMapper.readValue(message.getPayload(), ChatMessageRequestDto.class);

        LocalDateTime timestamp = (chatMessageRequestDto.getTimestamp() != null)
                ? OffsetDateTime.parse(chatMessageRequestDto.getTimestamp()).toLocalDateTime()
                : LocalDateTime.now();

        Chat chat = Chat.builder()
                .user(user)
                .sender(chatMessageRequestDto.getSender())
                .content(chatMessageRequestDto.getContent())
                .timestamp(timestamp)
                .build();

        chatRepository.save(chat);

        String responseJson = objectMapper.writeValueAsString(chatMessageRequestDto);

        // ✅ 모든 연결된 세션에 메시지 브로드캐스트
        for (WebSocketSession s : sessions.values()) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(responseJson));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        System.out.println("❌ 연결 종료: " + session.getId());
    }
}
