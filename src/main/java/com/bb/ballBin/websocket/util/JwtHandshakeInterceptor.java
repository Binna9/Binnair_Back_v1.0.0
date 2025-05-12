package com.bb.ballBin.websocket.util;

import com.bb.ballBin.security.jwt.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        URI uri = request.getURI();
        String query = uri.getQuery();

        if (query != null && query.startsWith("token=")) {

            String token = query.substring("token=".length());
            String userId = jwtUtil.getUserIdFromToken(token, false);

            if (userId != null) {
                attributes.put("userId", userId);
            }
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}

