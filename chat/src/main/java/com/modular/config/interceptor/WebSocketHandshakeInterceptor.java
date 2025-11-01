package com.modular.config.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
            // ws://localhost:8080/chat?memberId
            String query = request.getURI().getQuery();
            log.info("query = {}", query);

            if (query == null) {
                log.warn("No query parameters found in WebSocket handshake");
                return false;
            }

            Map<String, String> params = parseQuery(query);
            String memberIdStr = params.get("memberId");

            if (memberIdStr == null) {
                log.warn("Missing required parameters: memberId or roomId");
                return false;
            }

            try {
                Long memberId = Long.parseLong(memberIdStr);

                attributes.put("memberId", memberId);
                log.info("Handshake success: memberId={}", memberId);
                return true;

            } catch (NumberFormatException e) {
                log.error("Invalid parameter format: {}", query, e);
                return false;
            }

        } catch (Exception e) {
            log.error("Error in beforeHandshake", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket HandshakeInterceptor exception", exception);
        } else {
            log.info("WebSocket HandshakeInterceptor completed successfully");
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] parts = param.split("=", 2);
            if (parts.length == 2) {
                result.put(parts[0], parts[1]);
            }
        }
        return result;
    }
}
