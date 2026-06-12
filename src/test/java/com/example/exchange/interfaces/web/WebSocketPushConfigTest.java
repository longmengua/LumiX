/*
 * 檔案用途：測試 exchange multiplex WebSocket 的 private user subscription 與 cancel-on-disconnect 協議。
 */
package com.example.exchange.interfaces.web;

import com.example.exchange.application.service.CancelOnDisconnectService;
import com.example.exchange.application.service.PushGatewayService;
import com.example.exchange.interfaces.web.security.MarketDataStreamRateLimiter;
import com.example.exchange.interfaces.web.security.UserStreamSubscriptionAuthorizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketPushConfigTest {

    @Test
    @DisplayName("/ws/exchange subscribe.user 支援 auth、resume 與 cancel-on-disconnect 註冊")
    void exchangeSubscribeUserSupportsCancelOnDisconnectResume() throws Exception {
        PushGatewayService pushGatewayService = mock(PushGatewayService.class);
        CancelOnDisconnectService cancelOnDisconnectService = mock(CancelOnDisconnectService.class);
        UserStreamSubscriptionAuthorizer authorizer = mock(UserStreamSubscriptionAuthorizer.class);
        when(authorizer.authorize(42L, null, "test-token"))
                .thenReturn(new UserStreamSubscriptionAuthorizer.UserStreamAuthorizationDecision(
                        true,
                        HttpStatus.OK,
                        "ALLOWED"
                ));
        WebSocketPushConfig config = new WebSocketPushConfig(
                pushGatewayService,
                cancelOnDisconnectService,
                mock(MarketDataStreamRateLimiter.class),
                authorizer,
                new ObjectMapper()
        );
        TextWebSocketHandler handler = (TextWebSocketHandler) config.exchangeHandler();
        RecordingWebSocketSession session = new RecordingWebSocketSession("ws-new");

        // 場景：前端重連後用同一條 /ws/exchange 訂閱 private user，並把舊 connection id 轉移過來避免誤撤單。
        handler.handleMessage(session, new TextMessage("""
                {
                  "type": "subscribe.user",
                  "uid": 42,
                  "token": "test-token",
                  "symbol": "BTCUSDT",
                  "cancelOnDisconnect": true,
                  "resumeConnectionId": "ws-old"
                }
                """));
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(authorizer).authorize(42L, null, "test-token");
        verify(pushGatewayService).registerUserWebSocket(42L, session);
        verify(cancelOnDisconnectService).resume("ws-old", "ws-new", 42L);
        verify(cancelOnDisconnectService).register("ws-new", 42L, "BTCUSDT");
        verify(pushGatewayService).removeWebSocket(session);
        verify(cancelOnDisconnectService).cancelForConnection("ws-new");
    }

    private static final class RecordingWebSocketSession implements WebSocketSession {
        private final String id;
        private final Map<String, Object> attributes = new HashMap<>();

        private RecordingWebSocketSession(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public URI getUri() {
            return URI.create("ws://localhost/ws/exchange");
        }

        @Override
        public HttpHeaders getHandshakeHeaders() {
            return HttpHeaders.EMPTY;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public Principal getPrincipal() {
            return null;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public String getAcceptedProtocol() {
            return null;
        }

        @Override
        public void setTextMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getTextMessageSizeLimit() {
            return 8192;
        }

        @Override
        public void setBinaryMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getBinaryMessageSizeLimit() {
            return 8192;
        }

        @Override
        public List<WebSocketExtension> getExtensions() {
            return List.of();
        }

        @Override
        public void sendMessage(WebSocketMessage<?> message) throws IOException {
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void close(CloseStatus status) throws IOException {
        }
    }
}
