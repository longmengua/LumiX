/*
 * 檔案用途：測試 market-data SSE/WebSocket stream per-client 限流。
 */
package com.example.exchange.interfaces.web.security;

import com.example.exchange.infra.config.PushGatewayProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class MarketDataStreamRateLimiterTest {

    @Test
    @DisplayName("同一 client 與同一 stream 超過每分鐘訂閱上限時拒絕")
    void rejectsSameClientAndStreamWhenLimitExceeded() {
        PushGatewayProperties properties = properties(1);
        MarketDataStreamRateLimiter limiter = new MarketDataStreamRateLimiter(
                properties,
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)
        );

        // 場景：gateway 被同一 client 重複建立同一 market stream，第二次必須擋下。
        assertThat(limiter.consume(request("203.0.113.10"), "market", "BTCUSDT").allowed())
                .isTrue();
        MarketDataStreamRateLimiter.RateLimitDecision second =
                limiter.consume(request("203.0.113.10"), "market", "BTCUSDT");

        assertThat(second.allowed()).isFalse();
        assertThat(second.status().value()).isEqualTo(429);
        assertThat(second.reason()).isEqualTo("MARKET_DATA_STREAM_RATE_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("不同 client 或不同 stream 不共用限流額度")
    void separatesRateLimitByClientAndStream() {
        PushGatewayProperties properties = properties(1);
        MarketDataStreamRateLimiter limiter = new MarketDataStreamRateLimiter(
                properties,
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)
        );

        // 場景：同一分鐘內，不同 IP 與不同 stream 應有獨立 quota，避免誤傷正常訂閱。
        assertThat(limiter.consume(request("203.0.113.10"), "market", "BTCUSDT").allowed())
                .isTrue();
        assertThat(limiter.consume(request("203.0.113.11"), "market", "BTCUSDT").allowed())
                .isTrue();
        assertThat(limiter.consume(request("203.0.113.10"), "market", "ETHUSDT").allowed())
                .isTrue();
        assertThat(limiter.consume(request("203.0.113.10"), "user", "42").allowed())
                .isTrue();
    }

    @Test
    @DisplayName("X-Forwarded-For 只取第一個 client IP 做限流 key")
    void usesFirstForwardedIpForClientIdentity() {
        PushGatewayProperties properties = properties(1);
        MarketDataStreamRateLimiter limiter = new MarketDataStreamRateLimiter(
                properties,
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)
        );
        MockHttpServletRequest first = request("10.0.0.1");
        first.addHeader("X-Forwarded-For", "198.51.100.1, 10.0.0.1");
        MockHttpServletRequest second = request("10.0.0.2");
        second.addHeader("X-Forwarded-For", "198.51.100.1, 10.0.0.2");

        // 場景：反向代理後方 remoteAddr 不同時，仍要依真實 client IP 聚合限流。
        assertThat(limiter.consume(first, "market", "BTCUSDT").allowed()).isTrue();
        assertThat(limiter.consume(second, "market", "BTCUSDT").allowed()).isFalse();
    }

    private static PushGatewayProperties properties(int subscriptionsPerMinute) {
        PushGatewayProperties properties = new PushGatewayProperties();
        properties.getRateLimit().setSubscriptionsPerMinute(subscriptionsPerMinute);
        return properties;
    }

    private static MockHttpServletRequest request(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/market-data/BTCUSDT/stream");
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
