/*
 * 檔案用途：測試 push gateway 的 SSE/WebSocket heartbeat contract 與失效連線清理。
 */
package com.example.exchange.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PushGatewayServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("publishHeartbeat 會向 WebSocket client 發送 gateway.heartbeat contract")
    void publishHeartbeatSendsGatewayHeartbeatToWebSocketClients() throws Exception {
        PushGatewayService service = new PushGatewayService(objectMapper);
        RecordingWebSocketSession session = new RecordingWebSocketSession("ws-1", true);
        service.registerMarketWebSocket("btcusdt", session);

        // 場景：gateway 獨立部署時，client 可用固定 event 名稱與 timestamp 判斷連線仍有效。
        PushGatewayService.PushGatewayHeartbeatReport report =
                service.publishHeartbeat(Instant.parse("2026-06-01T00:00:00Z"));

        assertThat(session.messages).hasSize(1);
        JsonNode json = objectMapper.readTree(session.messages.getFirst());
        assertThat(json.path("event").asText()).isEqualTo(PushGatewayService.HEARTBEAT_EVENT);
        assertThat(json.path("data").path("channel").asText()).isEqualTo("market:BTCUSDT");
        assertThat(json.path("data").path("ts").asText()).isEqualTo("2026-06-01T00:00:00Z");
        assertThat(report.channels()).isEqualTo(1);
        assertThat(report.delivered()).isEqualTo(1);
        assertThat(report.failed()).isZero();
    }

    @Test
    @DisplayName("publishHeartbeat 會清理已關閉 WebSocket session")
    void publishHeartbeatRemovesClosedWebSocketSessions() throws Exception {
        PushGatewayService service = new PushGatewayService(objectMapper);
        RecordingWebSocketSession closed = new RecordingWebSocketSession("ws-closed", false);
        service.registerUserWebSocket(42L, closed);

        // 場景：client 已斷線時，heartbeat 不應嘗試送訊息，並要把 session 從 channel 移除。
        PushGatewayService.PushGatewayHeartbeatReport first =
                service.publishHeartbeat(Instant.parse("2026-06-01T00:00:00Z"));
        PushGatewayService.PushGatewayHeartbeatReport second =
                service.publishHeartbeat(Instant.parse("2026-06-01T00:00:30Z"));

        assertThat(closed.messages).isEmpty();
        assertThat(first.channels()).isEqualTo(1);
        assertThat(first.delivered()).isZero();
        assertThat(first.failed()).isEqualTo(1);
        assertThat(second.channels()).isEqualTo(1);
        assertThat(second.delivered()).isZero();
        assertThat(second.failed()).isZero();
    }

    private static final class RecordingWebSocketSession implements WebSocketSession {
        private final String id;
        private final boolean open;
        private final java.util.ArrayList<String> messages = new java.util.ArrayList<>();

        private RecordingWebSocketSession(String id, boolean open) {
            this.id = id;
            this.open = open;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public URI getUri() {
            return null;
        }

        @Override
        public HttpHeaders getHandshakeHeaders() {
            return HttpHeaders.EMPTY;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return Map.of();
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
            return 0;
        }

        @Override
        public void setBinaryMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getBinaryMessageSizeLimit() {
            return 0;
        }

        @Override
        public List<WebSocketExtension> getExtensions() {
            return List.of();
        }

        @Override
        public void sendMessage(WebSocketMessage<?> message) throws IOException {
            messages.add(((TextMessage) message).getPayload());
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void close(CloseStatus status) throws IOException {
        }
    }
}
