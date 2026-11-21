package com.lumix.marketfeed.binance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Binance USDⓈ-M 永續公開標記價及指數價接收器。只供後端風險判斷，
 * 不把外部委託或成交當作 LumiX 流動性；一般設定預設不啟動。
 */
@Component
@Profile("infrastructure")
@ConditionalOnProperty(prefix = "lumix.futures.reference-price", name = "enabled", havingValue = "true")
public class BinanceFuturesReferencePriceFeed {
    private static final Logger LOG = LoggerFactory.getLogger(BinanceFuturesReferencePriceFeed.class);
    private static final String SOURCE = "BINANCE_USDSM_PREMIUM_INDEX";
    private static final List<String> SYMBOLS = List.of("BTCUSDT", "ETHUSDT");
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SYMBOLS.size());
    private final Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();
    private final ObjectMapper json;

    /** 固定兩個來源商品和 URL，不能由 browser 或管理輸入指定外部主機。 */
    public BinanceFuturesReferencePriceFeed(ObjectMapper json) {
        this.json = json;
        for (String symbol : SYMBOLS) {
            scheduler.scheduleWithFixedDelay(() -> refresh(symbol), 0, 1, TimeUnit.SECONDS);
        }
    }

    /** 來源時間及本機接收時間都要在政策容忍範圍內；失敗時不提供舊價格。 */
    public Optional<Snapshot> current(String symbol, Duration maximumAge, Instant now) {
        if (!SYMBOLS.contains(symbol)) throw new IllegalArgumentException("unsupported futures symbol");
        if (maximumAge == null || maximumAge.isNegative() || maximumAge.isZero())
            throw new IllegalArgumentException("price freshness policy is required");
        return fresh(snapshots.get(symbol), maximumAge, now);
    }

    static Optional<Snapshot> fresh(Snapshot value, Duration maximumAge, Instant now) {
        if (value == null || now == null || now.isBefore(value.sourceAt())
                || now.isBefore(value.receivedAt())
                || Duration.between(value.sourceAt(), now).compareTo(maximumAge) > 0
                || Duration.between(value.receivedAt(), now).compareTo(maximumAge) > 0)
            return Optional.empty();
        return Optional.of(value);
    }

    private void refresh(String symbol) {
        try {
            URI uri = URI.create("https://fapi.binance.com/fapi/v1/premiumIndex?symbol=" + symbol);
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3)).GET().build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body().length > 4096)
                throw new IllegalStateException("reference price response is unavailable");
            Snapshot value = parse(json.readTree(response.body()), symbol, Instant.now());
            snapshots.compute(symbol, (ignored, previous) -> previous == null
                    || value.sourceAt().isAfter(previous.sourceAt()) ? value : previous);
        } catch (Exception failure) {
            snapshots.remove(symbol);
            LOG.warn("Futures reference price unavailable for {}: {}", symbol, failure.getClass().getSimpleName());
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
        }
    }

    /** 僅接受白名單商品、正數 plain-decimal 價格及可驗證的來源時間。 */
    static Snapshot parse(JsonNode payload, String expectedSymbol, Instant receivedAt) {
        if (!SYMBOLS.contains(expectedSymbol) || !expectedSymbol.equals(payload.path("symbol").asText()))
            throw new IllegalArgumentException("reference price symbol mismatch");
        BigDecimal mark = price(payload.path("markPrice"));
        BigDecimal index = price(payload.path("indexPrice"));
        JsonNode sourceTime = payload.path("time");
        if (!sourceTime.isIntegralNumber() || sourceTime.asLong() <= 0)
            throw new IllegalArgumentException("reference price source time is invalid");
        Instant sourceAt = Instant.ofEpochMilli(sourceTime.asLong());
        if (sourceAt.isAfter(receivedAt)) throw new IllegalArgumentException("reference price is from the future");
        return new Snapshot(SOURCE, expectedSymbol, mark, index, sourceAt, receivedAt);
    }

    private static BigDecimal price(JsonNode node) {
        String text = node.asText();
        if (!text.matches("(?:0|[1-9][0-9]{0,17})(?:\\.[0-9]{1,18})?"))
            throw new IllegalArgumentException("reference price is not a bounded decimal");
        BigDecimal value = new BigDecimal(text);
        if (value.signum() <= 0) throw new IllegalArgumentException("reference price must be positive");
        return value;
    }

    @PreDestroy
    public void close() {
        scheduler.shutdownNow();
        snapshots.clear();
    }

    /** 原始來源與時間隨價格提供給風險決策及日後的執行證據。 */
    public record Snapshot(String source, String symbol, BigDecimal markPrice, BigDecimal indexPrice,
                           Instant sourceAt, Instant receivedAt) { }
}
