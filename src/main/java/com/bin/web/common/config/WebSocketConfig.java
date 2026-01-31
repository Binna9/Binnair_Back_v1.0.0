package com.bin.web.common.config;

import com.bin.web.security.jwt.util.JwtUtil;
import com.bin.web.user.repository.UserRepository;
import com.bin.web.websocket.repository.ChatRepository;
import com.bin.web.websocket.util.JwtHandshakeInterceptor;
import com.bin.web.websocket.util.MyWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myHandler(), "/websocket")
                .addInterceptors(new JwtHandshakeInterceptor(jwtUtil))
                .setAllowedOrigins("*"); // CORS 허용
    }

    @Bean
    public WebSocketHandler myHandler() {
        return new MyWebSocketHandler(userRepository, chatRepository);
    }
}
