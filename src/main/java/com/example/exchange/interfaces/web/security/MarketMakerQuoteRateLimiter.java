/*
 * 檔案用途：限制做市商 quote command API 頻率，避免 replacement quote burst 反覆撤掛單。
 */
package com.example.exchange.interfaces.web.security;

import com.example.exchange.infra.config.MarketMakerApiProperties;
import com.example.exchange.interfaces.web.dto.MarketMakerQuoteRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MarketMakerQuoteRateLimiter {

    private final MarketMakerApiProperties properties;
    private final Clock clock;
    private final ConcurrentMap<String, WindowCounter> windows = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Autowired
    public MarketMakerQuoteRateLimiter(MarketMakerApiProperties properties) {
        this(properties, Clock.systemUTC());
    }

    MarketMakerQuoteRateLimiter(MarketMakerApiProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public RateLimitDecision consume(HttpServletRequest request, MarketMakerQuoteRequest quoteRequest) {
        return consume(
                request,
                quoteRequest == null ? null : quoteRequest.marketMakerId(),
                quoteRequest == null ? null : quoteRequest.symbol()
        );
    }

    RateLimitDecision consume(HttpServletRequest request, String marketMakerId, String symbol) {
        return consume(
                resolveClientIp(request),
                marketMakerId,
                symbol
        );
    }

    RateLimitDecision consume(String clientIp, String marketMakerId, String symbol) {
        MarketMakerApiProperties.QuoteRateLimit rateLimit = properties.getQuoteRateLimit();
        if (!rateLimit.isEnabled()) {
            return RateLimitDecision.allow();
        }

        long minuteBucket = Instant.now(clock).getEpochSecond() / 60;
        String key = normalize(clientIp) + ":" + normalize(marketMakerId) + ":" + normalize(symbol);
        cleanupIfNeeded(rateLimit.getMaxTrackedKeys());
        WindowCounter counter = windows.computeIfAbsent(key, ignored -> new WindowCounter(minuteBucket));

        synchronized (counter) {
            if (counter.minuteBucket != minuteBucket) {
                counter.minuteBucket = minuteBucket;
                counter.count = 0;
            }

            counter.count++;
            counter.lastSeenSequence = sequence.incrementAndGet();

            if (counter.count <= rateLimit.getQuotesPerMinute()) {
                return RateLimitDecision.allow();
            }
            return RateLimitDecision.rejected(HttpStatus.TOO_MANY_REQUESTS, "MARKET_MAKER_QUOTE_RATE_LIMIT_EXCEEDED");
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
        String configuredHeader = properties.getQuoteRateLimit().getClientIpHeader();
        if (configuredHeader != null && !configuredHeader.isBlank()) {
            String forwarded = request.getHeader(configuredHeader);
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",", 2)[0].trim();
            }
        }
        return request.getRemoteAddr();
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
