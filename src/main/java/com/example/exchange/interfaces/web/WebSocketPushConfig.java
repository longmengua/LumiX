package com.example.exchange.interfaces.web;

import com.example.exchange.application.service.PushGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketPushConfig implements WebSocketConfigurer {

    private final PushGatewayService pushGatewayService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(marketHandler(), "/ws/market/*").setAllowedOrigins("*");
        registry.addHandler(userHandler(), "/ws/user/*").setAllowedOrigins("*");
    }

    private WebSocketHandler marketHandler() {
        return new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                pushGatewayService.registerMarketWebSocket(lastPathSegment(session), session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                pushGatewayService.removeWebSocket(session);
            }
        };
    }

    private WebSocketHandler userHandler() {
        return new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                pushGatewayService.registerUserWebSocket(Long.parseLong(lastPathSegment(session)), session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                pushGatewayService.removeWebSocket(session);
            }
        };
    }

    private static String lastPathSegment(WebSocketSession session) {
        String path = session.getUri() == null ? "" : session.getUri().getPath();
        int index = path.lastIndexOf('/');
        return index < 0 ? path : path.substring(index + 1);
    }
}
