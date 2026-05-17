/*
 * 檔案用途：Java 原始碼檔案，屬於 java21-match-hub 交易服務。
 */
package com.example.exchange.interfaces.web;

import com.example.exchange.application.service.CancelOnDisconnectService;
import com.example.exchange.application.service.PushGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketPushConfig implements WebSocketConfigurer {

    private final PushGatewayService pushGatewayService;
    private final CancelOnDisconnectService cancelOnDisconnectService;

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
                long uid = Long.parseLong(lastPathSegment(session));
                pushGatewayService.registerUserWebSocket(uid, session);
                if (cancelOnDisconnectEnabled(session)) {
                    cancelOnDisconnectService.register(session.getId(), uid, cancelOnDisconnectSymbol(session));
                }
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                pushGatewayService.removeWebSocket(session);
                cancelOnDisconnectService.cancelForConnection(session.getId());
            }
        };
    }

    private static String lastPathSegment(WebSocketSession session) {
        String path = session.getUri() == null ? "" : session.getUri().getPath();
        int index = path.lastIndexOf('/');
        return index < 0 ? path : path.substring(index + 1);
    }

    private static boolean cancelOnDisconnectEnabled(WebSocketSession session) {
        return Boolean.parseBoolean(queryParams(session).getFirst("cancelOnDisconnect"));
    }

    private static String cancelOnDisconnectSymbol(WebSocketSession session) {
        return queryParams(session).getFirst("symbol");
    }

    private static MultiValueMap<String, String> queryParams(WebSocketSession session) {
        if (session.getUri() == null) {
            return new LinkedMultiValueMap<>();
        }
        return UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams();
    }
}
