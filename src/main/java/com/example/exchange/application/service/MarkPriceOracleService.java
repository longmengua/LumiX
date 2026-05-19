/*
 * 檔案用途：應用服務，提供 mark/index price oracle baseline。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MarkPriceSnapshot;
import com.example.exchange.infra.config.MarkPriceOracleProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MarkPriceOracleService {

    private final MarkPriceOracleProperties properties;
    private final Map<String, Quote> quotes = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadConfiguredPrices() {
        for (MarkPriceOracleProperties.Price price : properties.getPrices()) {
            if (price == null || price.getSymbol() == null || price.getSymbol().isBlank()) {
                continue;
            }
            update(
                    price.getSymbol(),
                    price.getMarkPrice(),
                    price.getIndexPrice(),
                    price.getSource()
            );
        }
    }

    public MarkPriceSnapshot update(String symbol, BigDecimal markPrice, BigDecimal indexPrice, String source) {
        String normalizedSymbol = normalizeSymbol(symbol);
        requirePositive(markPrice, "markPrice");
        requirePositive(indexPrice, "indexPrice");
        Quote quote = new Quote(
                normalizedSymbol,
                markPrice,
                indexPrice,
                source == null || source.isBlank() ? "manual" : source.trim(),
                Instant.now()
        );
        quotes.put(normalizedSymbol, quote);
        return toSnapshot(quote);
    }

    public Optional<MarkPriceSnapshot> snapshot(String symbol) {
        Quote quote = quotes.get(normalizeSymbol(symbol));
        return quote == null ? Optional.empty() : Optional.of(toSnapshot(quote));
    }

    public MarkPriceSnapshot requireFresh(String symbol) {
        MarkPriceSnapshot snapshot = snapshot(symbol)
                .orElseThrow(() -> new IllegalStateException("missing mark price oracle input: " + normalizeSymbol(symbol)));
        if (snapshot.stale()) {
            throw new IllegalStateException("stale mark price oracle input: " + normalizeSymbol(symbol));
        }
        return snapshot;
    }

    public BigDecimal requireMarkPrice(String symbol) {
        return requireFresh(symbol).markPrice();
    }

    private MarkPriceSnapshot toSnapshot(Quote quote) {
        return new MarkPriceSnapshot(
                quote.symbol(),
                quote.markPrice(),
                quote.indexPrice(),
                quote.source(),
                quote.updatedAt(),
                isStale(quote.updatedAt())
        );
    }

    private boolean isStale(Instant updatedAt) {
        long maxStalenessMs = Math.max(1, properties.getMaxStalenessMs());
        return updatedAt.plus(Duration.ofMillis(maxStalenessMs)).isBefore(Instant.now());
    }

    private static void requirePositive(BigDecimal value, String name) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol cannot be blank");
        }
        return symbol.trim().toUpperCase();
    }

    private record Quote(
            String symbol,
            BigDecimal markPrice,
            BigDecimal indexPrice,
            String source,
            Instant updatedAt
    ) {
    }
}
