/*
 * 檔案用途：限制 market-data SSE/WebSocket client 建立 stream 的頻率。
 */
package com.example.exchange.interfaces.web.security;

import com.example.exchange.infra.config.PushGatewayProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MarketDataStreamRateLimiter {

    private final PushGatewayProperties properties;
    private final Clock clock;
    private final ConcurrentMap<String, WindowCounter> windows = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public MarketDataStreamRateLimiter(PushGatewayProperties properties) {
        this(properties, Clock.systemUTC());
    }

    MarketDataStreamRateLimiter(PushGatewayProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public RateLimitDecision consume(HttpServletRequest request, String streamType, String streamId) {
        return consume(resolveClientIp(request), streamType, streamId);
    }

    public RateLimitDecision consume(ServerHttpRequest request, String streamType, String streamId) {
        return consume(resolveClientIp(request), streamType, streamId);
    }

    RateLimitDecision consume(String clientIp, String streamType, String streamId) {
        PushGatewayProperties.RateLimit rateLimit = properties.getRateLimit();
        if (!rateLimit.isEnabled()) {
            return RateLimitDecision.allow();
        }

        long minuteBucket = Instant.now(clock).getEpochSecond() / 60;
        String key = normalize(clientIp) + ":" + normalize(streamType) + ":" + normalize(streamId);
        cleanupIfNeeded(rateLimit.getMaxTrackedKeys());
        WindowCounter counter = windows.computeIfAbsent(key, ignored -> new WindowCounter(minuteBucket));

        synchronized (counter) {
            if (counter.minuteBucket != minuteBucket) {
                counter.minuteBucket = minuteBucket;
                counter.count = 0;
            }

            counter.count++;
            counter.lastSeenSequence = sequence.incrementAndGet();

            if (counter.count <= rateLimit.getSubscriptionsPerMinute()) {
                return RateLimitDecision.allow();
            }
            return RateLimitDecision.rejected(HttpStatus.TOO_MANY_REQUESTS, "MARKET_DATA_STREAM_RATE_LIMIT_EXCEEDED");
        }
    }

    private void cleanupIfNeeded(int maxTrackedKeys) {
        if (windows.size() <= maxTrackedKeys) {
            return;
        }

        long threshold = sequence.get() - maxTrackedKeys;
        windows.entrySet().removeIf(entry -> entry.getValue().lastSeenSequence < threshold);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String configuredHeader = properties.getRateLimit().getClientIpHeader();
        if (configuredHeader != null && !configuredHeader.isBlank()) {
            String forwarded = request.getHeader(configuredHeader);
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",", 2)[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String configuredHeader = properties.getRateLimit().getClientIpHeader();
        if (configuredHeader != null && !configuredHeader.isBlank()) {
            String forwarded = request.getHeaders().getFirst(configuredHeader);
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",", 2)[0].trim();
            }
        }
        return request.getRemoteAddress() == null
                ? "unknown"
                : request.getRemoteAddress().getAddress().getHostAddress();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim().toUpperCase();
    }

    private static final class WindowCounter {
        private long minuteBucket;
        private int count;
        private long lastSeenSequence;

        private WindowCounter(long minuteBucket) {
            this.minuteBucket = minuteBucket;
        }
    }

    public record RateLimitDecision(
            boolean allowed,
            HttpStatus status,
            String reason
    ) {
        private static RateLimitDecision allow() {
            return new RateLimitDecision(true, HttpStatus.OK, "ALLOWED");
        }

        private static RateLimitDecision rejected(HttpStatus status, String reason) {
            return new RateLimitDecision(false, status, reason);
        }
    }
}
