/*
 * 檔案用途：測試做市商 hedge execution API frequency limit policy。
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

class MarketMakerHedgeExecutionRateLimiterTest {

    @Test
    @DisplayName("同一 client 與 execution scope 超過每分鐘 hedge execution 上限時拒絕")
    void rejectsSameClientAndExecutionScopeWhenLimitExceeded() {
        MarketMakerHedgeExecutionRateLimiter limiter = new MarketMakerHedgeExecutionRateLimiter(
                properties(1),
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)
        );

        // 場景：manual hedge execution 可能送出外部 venue orders，同一 scope 的 burst 必須擋在 execution service 前。
        assertThat(limiter.consume(request("203.0.113.10"), "mm-1").allowed())
                .isTrue();
        MarketMakerHedgeExecutionRateLimiter.RateLimitDecision second =
                limiter.consume(request("203.0.113.10"), "mm-1");

        assertThat(second.allowed()).isFalse();
        assertThat(second.status().value()).isEqualTo(429);
        assertThat(second.reason()).isEqualTo("MARKET_MAKER_HEDGE_EXECUTION_RATE_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("不同 client 或 execution scope 不共用 hedge execution 限流額度")
    void separatesRateLimitByClientAndExecutionScope() {
        MarketMakerHedgeExecutionRateLimiter limiter = new MarketMakerHedgeExecutionRateLimiter(
                properties(1),
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)
        );

        // 場景：單一做市商 manual execution 與 enabled batch execution 是不同操作面，quota 不應互相消耗。
        assertThat(limiter.consume(request("203.0.113.10"), "mm-1").allowed())
                .isTrue();
        assertThat(limiter.consume(request("203.0.113.11"), "mm-1").allowed())
                .isTrue();
        assertThat(limiter.consume(request("203.0.113.10"), "mm-2").allowed())
                .isTrue();
        assertThat(limiter.consume(
                        request("203.0.113.10"),
                        MarketMakerHedgeExecutionRateLimiter.ENABLED_MARKET_MAKERS_SCOPE
                ).allowed())
                .isTrue();
    }

    @Test
    @DisplayName("X-Forwarded-For 只取第一個 client IP 做 hedge execution 限流 key")
    void usesFirstForwardedIpForClientIdentity() {
        MarketMakerHedgeExecutionRateLimiter limiter = new MarketMakerHedgeExecutionRateLimiter(
                properties(1),
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)
        );
        MockHttpServletRequest first = request("10.0.0.1");
        first.addHeader("X-Forwarded-For", "198.51.100.1, 10.0.0.1");
        MockHttpServletRequest second = request("10.0.0.2");
        second.addHeader("X-Forwarded-For", "198.51.100.1, 10.0.0.2");

        // 場景：反向代理後方 remoteAddr 不同時，仍要依真實 client IP 聚合同一 execution scope。
        assertThat(limiter.consume(first, "mm-1").allowed()).isTrue();
        assertThat(limiter.consume(second, "mm-1").allowed()).isFalse();
    }

    @Test
    @DisplayName("停用 hedge execution 限流時不消耗 quota")
    void allowsWhenDisabled() {
        MarketMakerApiProperties properties = properties(1);
        properties.getHedgeExecutionRateLimit().setEnabled(false);
        MarketMakerHedgeExecutionRateLimiter limiter = new MarketMakerHedgeExecutionRateLimiter(
                properties,
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)
        );

        assertThat(limiter.consume(request("203.0.113.10"), "mm-1").allowed()).isTrue();
        assertThat(limiter.consume(request("203.0.113.10"), "mm-1").allowed()).isTrue();
    }

    private static MarketMakerApiProperties properties(int executionsPerMinute) {
        MarketMakerApiProperties properties = new MarketMakerApiProperties();
        properties.getHedgeExecutionRateLimit().setExecutionsPerMinute(executionsPerMinute);
        return properties;
    }

    private static MockHttpServletRequest request(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/market-maker/hedge-execution");
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
