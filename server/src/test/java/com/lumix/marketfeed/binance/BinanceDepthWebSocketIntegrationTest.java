package com.lumix.marketfeed.binance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.application.UserAuthenticationService;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** 傳輸層僅使用測試來源，不連正式上游或 application runtime。 */
@SpringBootTest(classes = BinanceDepthWebSocketIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"lumix.market-feed.binance.enabled=true", "lumix.market-feed.binance.allowed-origin=https://localhost",
                "server.forward-headers-strategy=framework"})
@ActiveProfiles("infrastructure")
class BinanceDepthWebSocketIntegrationTest {
    @LocalServerPort int port;

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
    @Import({BinanceDepthWebSocket.class, ApiAuthenticationFilter.class})
    static class TestApplication {
        @Bean BinanceDepthFeed feed() {
            BinanceDepthFeed feed = mock(BinanceDepthFeed.class);
            when(feed.view(anyString())).thenReturn(new BinanceDepthFeed.FeedView(false, 0, null, null, List.of(), List.of()));
            return feed;
        }
        @Bean BinanceKlineFeed klines() {
            BinanceKlineFeed feed = mock(BinanceKlineFeed.class);
            when(feed.view(anyString())).thenReturn(new BinanceKlineSeries.View(true, "1m", null, null,
                    List.of(new BinanceKlineSeries.Candle(0, "100.00000001", "102", "99", "101", "1", 1, false))));
            return feed;
        }
        @Bean UserAuthenticationProperties authProperties() { return new UserAuthenticationProperties(); }
        @Bean UserAuthenticationService authenticationService() {
            UserAuthenticationService service = mock(UserAuthenticationService.class);
            when(service.authenticate(any())).thenReturn(new AuthenticatedUser("user", "user@example.test", "user"));
            return service;
        }
    }

    @Test
    void pushesUnavailableStateOverForwardedSecureHandshake() throws Exception {
        // 實際註冊認證 filter；測試 cookie 經 mock 認證服務後才能通過握手。
        CompletableFuture<String> received = new CompletableFuture<>();
        WebSocket socket = HttpClient.newHttpClient().newWebSocketBuilder()
                .header("Origin", "https://localhost")
                .header("X-Forwarded-Proto", "https")
                .header("Cookie", "LUMIX_SESSION=00000000-0000-0000-0000-000000000001.test-secret")
                .connectTimeout(Duration.ofSeconds(5))
                .buildAsync(URI.create("ws://127.0.0.1:" + port + "/api/v1/market-data/stream"), new WebSocket.Listener() {
                    @Override public void onOpen(WebSocket ws) { ws.request(1); }
                    @Override public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                        if (last) received.complete(data.toString());
                        ws.request(1);
                        return null;
                    }
                }).get(5, TimeUnit.SECONDS);
        try {
            JsonNode message = new ObjectMapper().readTree(received.get(5, TimeUnit.SECONDS));
            assertEquals("BINANCE_SPOT", message.path("source").asText());
            assertFalse(message.path("BTCUSDT").path("healthy").asBoolean(true));
            assertTrue(message.path("BTCUSDT").path("bids").isEmpty());
            assertFalse(message.path("ETHUSDT").path("healthy").asBoolean(true));
            assertTrue(message.path("candles").path("BTCUSDT").path("healthy").asBoolean(false));
            assertEquals("100.00000001", message.path("candles").path("BTCUSDT").path("candles").get(0).path("open").asText());
        } finally { socket.abort(); }
    }

    @Test
    void missingSessionCannotOpenStream() {
        // 缺少 session 的連線不得繞過 API filter 取得行情推播。
        var exception = assertThrows(java.util.concurrent.ExecutionException.class, () -> HttpClient.newHttpClient()
                .newWebSocketBuilder().header("Origin", "https://localhost").header("X-Forwarded-Proto", "https")
                .connectTimeout(Duration.ofSeconds(5))
                .buildAsync(URI.create("ws://127.0.0.1:" + port + "/api/v1/market-data/stream"), new WebSocket.Listener() { })
                .get(5, TimeUnit.SECONDS));
        assertInstanceOf(WebSocketHandshakeException.class, exception.getCause());
        assertEquals(401, ((WebSocketHandshakeException) exception.getCause()).getResponse().statusCode());
    }
}
