package com.bb.ballBin.common.config;

import com.bb.ballBin.security.jwt.util.JwtUtil;
import com.bb.ballBin.user.repository.UserRepository;
import com.bb.ballBin.websocket.repository.ChatRepository;
import com.bb.ballBin.websocket.util.JwtHandshakeInterceptor;
import com.bb.ballBin.websocket.util.MyWebSocketHandler;
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
