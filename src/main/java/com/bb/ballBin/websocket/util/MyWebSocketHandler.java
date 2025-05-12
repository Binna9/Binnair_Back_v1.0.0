package com.bb.ballBin.websocket.util;

import com.bb.ballBin.websocket.model.ChatMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MyWebSocketHandler extends TextWebSocketHandler {

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

        String payload = message.getPayload();
        // JSON 문자열 → ChatMessage 객체로 변환
        ChatMessageDto chatMessage = objectMapper.readValue(payload, ChatMessageDto.class);

        System.out.println("💬 메시지 수신:");
        System.out.println("  - sender: " + chatMessage.getSender());
        System.out.println("  - content: " + chatMessage.getContent());
        System.out.println("  - time: " + chatMessage.getTimestamp());

        String responseJson = objectMapper.writeValueAsString(chatMessage);

        // ✅ 모든 연결된 세션에 메시지 브로드캐스트
        for (WebSocketSession s : sessions.values()) {
            if (s.isOpen()) {
                try {
                    s.sendMessage(new TextMessage(responseJson));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        System.out.println("❌ 연결 종료: " + session.getId());
    }
}
