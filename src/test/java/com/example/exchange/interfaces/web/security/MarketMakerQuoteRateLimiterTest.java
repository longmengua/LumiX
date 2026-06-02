/*
 * 檔案用途：測試做市商 quote command API frequency limit policy。
 */
package com.example.exchange.interfaces.web.security;

import com.example.exchange.infra.config.MarketMakerApiProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class MarketMakerQuoteRateLimiterTest {

    @Test
    @DisplayName("同一 client、market-maker 與 symbol 超過每分鐘 quote 上限時拒絕")
    void rejectsSameClientMarketMakerAndSymbolWhenLimitExceeded() {
        MarketMakerQuoteRateLimiter limiter = new MarketMakerQuoteRateLimiter(
                properties(1),
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)
        );

        // 場景：quote replacement 會先撤舊單再掛新單，因此同一 client 對同一做市商與 symbol 的 burst 必須擋在 service 前。
        assertThat(limiter.consume(request("203.0.113.10"), "mm-1", "BTCUSDT").allowed())
                .isTrue();
        MarketMakerQuoteRateLimiter.RateLimitDecision second =
                limiter.consume(request("203.0.113.10"), "mm-1", "BTCUSDT");

        assertThat(second.allowed()).isFalse();
        assertThat(second.status().value()).isEqualTo(429);
        assertThat(second.reason()).isEqualTo("MARKET_MAKER_QUOTE_RATE_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("不同 client、market-maker 或 symbol 不共用 quote 限流額度")
    void separatesRateLimitByClientMarketMakerAndSymbol() {
        MarketMakerQuoteRateLimiter limiter = new MarketMakerQuoteRateLimiter(
                properties(1),
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)
        );

        // 場景：多做市商或多市場同步報價時，限流 key 應避免互相誤傷。
        assertThat(limiter.consume(request("203.0.113.10"), "mm-1", "BTCUSDT").allowed())
                .isTrue();
        assertThat(limiter.consume(request("203.0.113.11"), "mm-1", "BTCUSDT").allowed())
                .isTrue();
        assertThat(limiter.consume(request("203.0.113.10"), "mm-2", "BTCUSDT").allowed())
                .isTrue();
        assertThat(limiter.consume(request("203.0.113.10"), "mm-1", "ETHUSDT").allowed())
                .isTrue();
    }

    @Test
    @DisplayName("X-Forwarded-For 只取第一個 client IP 做 quote 限流 key")
    void usesFirstForwardedIpForClientIdentity() {
        MarketMakerQuoteRateLimiter limiter = new MarketMakerQuoteRateLimiter(
                properties(1),
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)
        );
        MockHttpServletRequest first = request("10.0.0.1");
        first.addHeader("X-Forwarded-For", "198.51.100.1, 10.0.0.1");
        MockHttpServletRequest second = request("10.0.0.2");
        second.addHeader("X-Forwarded-For", "198.51.100.1, 10.0.0.2");

        // 場景：gateway 或 LB 後方 remoteAddr 不穩定時，仍要依真實 client IP 聚合 quote command quota。
        assertThat(limiter.consume(first, "mm-1", "BTCUSDT").allowed()).isTrue();
        assertThat(limiter.consume(second, "mm-1", "BTCUSDT").allowed()).isFalse();
    }

    @Test
    @DisplayName("停用 quote 限流時不消耗 quota")
    void allowsWhenDisabled() {
        MarketMakerApiProperties properties = properties(1);
        properties.getQuoteRateLimit().setEnabled(false);
        MarketMakerQuoteRateLimiter limiter = new MarketMakerQuoteRateLimiter(
                properties,
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)
        );

        assertThat(limiter.consume(request("203.0.113.10"), "mm-1", "BTCUSDT").allowed()).isTrue();
        assertThat(limiter.consume(request("203.0.113.10"), "mm-1", "BTCUSDT").allowed()).isTrue();
    }

    private static MarketMakerApiProperties properties(int quotesPerMinute) {
        MarketMakerApiProperties properties = new MarketMakerApiProperties();
        properties.getQuoteRateLimit().setQuotesPerMinute(quotesPerMinute);
        return properties;
    }

    private static MockHttpServletRequest request(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/market-maker/quotes");
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
