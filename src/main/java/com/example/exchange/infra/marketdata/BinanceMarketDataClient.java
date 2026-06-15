/*
 * 檔案用途：基礎設施 adapter，讀取 Binance Futures public market-data 作為前台參考價格來源。
 */
package com.example.exchange.infra.marketdata;

import com.example.exchange.domain.model.dto.MarketKline;
import com.example.exchange.domain.model.dto.MarketTicker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class BinanceMarketDataClient {

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public BinanceMarketDataClient(
            OkHttpClient okHttpClient,
            ObjectMapper objectMapper,
            @Value("${binance.futures.base-url:https://fapi.binance.com}") String baseUrl
    ) {
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://fapi.binance.com" : baseUrl;
    }

    public Optional<MarketTicker> ticker(String symbol) {
        HttpUrl url = urlBuilder("/fapi/v1/ticker/24hr")
                .addQueryParameter("symbol", normalize(symbol))
                .build();
        return get(url).map(this::tickerFromJson);
    }

    public List<MarketKline> klines(String symbol, int limit) {
        HttpUrl url = urlBuilder("/fapi/v1/klines")
                .addQueryParameter("symbol", normalize(symbol))
                .addQueryParameter("interval", "1m")
                .addQueryParameter("limit", Integer.toString(Math.max(1, Math.min(1000, limit))))
                .build();
        return get(url).map(root -> klinesFromJson(normalize(symbol), root)).orElseGet(List::of);
    }

    private Optional<JsonNode> get(HttpUrl url) {
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readTree(response.body().string()));
        } catch (IOException | RuntimeException ex) {
            // Binance is a reference-data dependency; callers fall back to local market-data on failures.
            return Optional.empty();
        }
    }

    private MarketTicker tickerFromJson(JsonNode root) {
        Instant updatedAt = millis(root, "closeTime")
                .map(Instant::ofEpochMilli)
                .orElseGet(Instant::now);
        return new MarketTicker(
                text(root, "symbol").orElse(""),
                decimal(root, "lastPrice").orElse(null),
                decimal(root, "bidPrice").orElse(null),
                decimal(root, "askPrice").orElse(null),
                decimal(root, "volume").orElse(BigDecimal.ZERO),
                decimal(root, "highPrice").orElse(null),
                decimal(root, "lowPrice").orElse(null),
                updatedAt
        );
    }

    private List<MarketKline> klinesFromJson(String symbol, JsonNode root) {
        List<MarketKline> result = new ArrayList<>();
        if (root == null || !root.isArray()) {
            return result;
        }
        for (JsonNode row : root) {
            if (!row.isArray() || row.size() < 6) {
                continue;
            }
            result.add(new MarketKline(
                    symbol,
                    "1m",
                    Instant.ofEpochMilli(row.get(0).asLong()),
                    decimal(row.get(1)).orElse(BigDecimal.ZERO),
                    decimal(row.get(2)).orElse(BigDecimal.ZERO),
                    decimal(row.get(3)).orElse(BigDecimal.ZERO),
                    decimal(row.get(4)).orElse(BigDecimal.ZERO),
                    decimal(row.get(5)).orElse(BigDecimal.ZERO)
            ));
        }
        return result;
    }

    private HttpUrl.Builder urlBuilder(String path) {
        HttpUrl parsed = HttpUrl.parse(baseUrl);
        if (parsed == null) {
            parsed = HttpUrl.parse("https://fapi.binance.com");
        }
        return parsed.newBuilder().encodedPath(path);
    }

    private static String normalize(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }

    private static Optional<String> text(JsonNode root, String field) {
        JsonNode value = root == null ? null : root.get(field);
        return value == null || value.isNull() ? Optional.empty() : Optional.of(value.asText());
    }

    private static Optional<BigDecimal> decimal(JsonNode root, String field) {
        JsonNode value = root == null ? null : root.get(field);
        return decimal(value);
    }

    private static Optional<BigDecimal> decimal(JsonNode value) {
        if (value == null || value.isNull()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(value.asText()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static Optional<Long> millis(JsonNode root, String field) {
        JsonNode value = root == null ? null : root.get(field);
        return value == null || value.isNull() ? Optional.empty() : Optional.of(value.asLong());
    }
}
