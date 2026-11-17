package com.lumix.marketfeed.binance;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/** 已認證使用者的唯讀行情推播；對外 TLS 由受信任 Nginx 終結。 */
@Configuration
@EnableWebSocket
@Profile("infrastructure")
@ConditionalOnProperty(prefix = "lumix.market-feed.binance", name = "enabled", havingValue = "true")
public class BinanceDepthWebSocket implements WebSocketConfigurer {
    private final Handler handler;
    private final String origin;

    /** 未設定具名 HTTPS origin 時拒絕啟動，避免意外開放跨站握手。 */
    public BinanceDepthWebSocket(BinanceDepthFeed feed, BinanceKlineFeed klines, ObjectMapper mapper,
            @Value("${lumix.market-feed.binance.allowed-origin:}") String origin) {
        if (!origin.matches("https://[A-Za-z0-9.-]+(?::[0-9]{1,5})?")) throw new IllegalArgumentException("market feed requires explicit HTTPS origin");
        this.origin = origin;
        this.handler = new Handler(feed, klines, mapper);
    }

    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/v1/market-data/stream").addInterceptors(new SecureHandshake()).setAllowedOrigins(origin);
    }

    /** 結束程序時釋放排程與連線。 */
    @PreDestroy public void close() { handler.close(); }

    private static final class SecureHandshake implements HandshakeInterceptor {
        @Override public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Map<String, Object> attributes) {
            // 只信任受控反向代理設定的安全狀態；後端 port 不可直接對公網開放。
            if (!(request instanceof ServletServerHttpRequest servlet) || !servlet.getServletRequest().isSecure()) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }
            return true;
        }
        @Override public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Exception exception) { }
    }

    private static final class Handler extends TextWebSocketHandler {
        private record Connection(WebSocketSession session, long openedAtNanos) { }
        private final BinanceDepthFeed feed;
        private final BinanceKlineFeed klines;
        private final ObjectMapper mapper;
        private final Map<String, Connection> sessions = new ConcurrentHashMap<>();
        private final ScheduledExecutorService publisher = Executors.newSingleThreadScheduledExecutor();

        private Handler(BinanceDepthFeed feed, BinanceKlineFeed klines, ObjectMapper mapper) {
            this.feed = feed;
            this.klines = klines;
            this.mapper = mapper;
            publisher.scheduleWithFixedDelay(this::publish, 0, 1, TimeUnit.SECONDS);
        }

        @Override public synchronized void afterConnectionEstablished(WebSocketSession session) throws Exception {
            if (sessions.size() >= 100) { session.close(CloseStatus.POLICY_VIOLATION); return; }
            session.setTextMessageSizeLimit(1024);
            sessions.put(session.getId(), new Connection(new ConcurrentWebSocketSessionDecorator(session, 2000, 65536), System.nanoTime()));
        }
        @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            // 此端點固定推播兩個交易對，不接受客戶端任意訂閱或指令。
            session.close(CloseStatus.POLICY_VIOLATION);
        }
        @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { sessions.remove(session.getId()); }
        @Override public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            sessions.remove(session.getId());
            session.close(CloseStatus.SERVER_ERROR);
        }
        private void publish() {
            try {
                String payload = mapper.writeValueAsString(Map.of("source", "BINANCE_SPOT", "publishedAt", Instant.now().toString(),
                        "BTCUSDT", feed.view("BTCUSDT"), "ETHUSDT", feed.view("ETHUSDT"),
                        "candles", Map.of("BTCUSDT", klines.view("BTCUSDT"), "ETHUSDT", klines.view("ETHUSDT"))));
                for (Connection connection : sessions.values()) {
                    WebSocketSession session = connection.session();
                    // 長連線固定重新握手，讓登出或過期的 session 不能無限期保留資料通道。
                    if (System.nanoTime() - connection.openedAtNanos() >= TimeUnit.MINUTES.toNanos(1)) {
                        sessions.remove(session.getId());
                        try { session.close(CloseStatus.NORMAL); } catch (IOException ignored) { }
                        continue;
                    }
                    try { session.sendMessage(new TextMessage(payload)); }
                    catch (IOException | RuntimeException exception) { sessions.remove(session.getId()); try { session.close(CloseStatus.SERVER_ERROR); } catch (IOException ignored) { } }
                }
            } catch (Exception exception) {
                // 無法產生完整快照時關閉連線，不得持續呈現最後一筆舊行情。
                sessions.values().forEach(connection -> { try { connection.session().close(CloseStatus.SERVER_ERROR); } catch (IOException ignored) { } });
                sessions.clear();
            }
        }
        private void close() { publisher.shutdownNow(); sessions.values().forEach(connection -> { try { connection.session().close(CloseStatus.GOING_AWAY); } catch (IOException ignored) { } }); sessions.clear(); }
    }
}
