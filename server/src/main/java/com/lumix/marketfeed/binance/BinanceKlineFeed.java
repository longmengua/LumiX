package com.lumix.marketfeed.binance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Binance 七種具名週期 K 線接收器；僅由後端建立受 TLS 驗證的外部連線。 */
@Component
@Profile("infrastructure")
@ConditionalOnProperty(prefix = "lumix.market-feed.binance", name = "enabled", havingValue = "true")
public class BinanceKlineFeed {
    private static final Logger LOG = LoggerFactory.getLogger(BinanceKlineFeed.class);
    private static final int MAX_BYTES = 256_000;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, Stream> streams = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    private volatile boolean closed;

    /** 限定本階段已選定交易對，避免使用者輸入控制外部連線。 */
    public BinanceKlineFeed(ObjectMapper mapper) {
        this.mapper = mapper;
        for (String symbol : List.of("BTCUSDT", "ETHUSDT")) {
            // 兩個固定交易對乘七個週期，來源連線數固定為十四，不受訂閱者輸入擴增。
            for (BinanceKlineInterval interval : BinanceKlineInterval.values()) {
                Stream stream = new Stream(symbol, interval);
                streams.put(symbol + "|" + interval.value(), stream);
                scheduler.execute(stream::connect);
                scheduler.scheduleWithFixedDelay(stream::checkLiveness, 5, 5, TimeUnit.SECONDS);
            }
        }
    }

    /** 只回傳已同步且未過期的來源 K 線。 */
    public BinanceKlineSeries.View view(String symbol, BinanceKlineInterval interval) {
        Stream stream = streams.get(symbol + "|" + interval.value());
        if (stream == null) throw new IllegalArgumentException("unsupported symbol");
        return stream.series.view(Instant.now());
    }

    /** 應用程式停止時結束所有來源連線。 */
    @PreDestroy public void close() {
        closed = true;
        streams.values().forEach(Stream::stop);
        scheduler.shutdownNow();
    }

    private record Update(BinanceKlineSeries.Candle candle, Instant sourceAt, Instant receivedAt) { }

    private final class Stream {
        private final String symbol;
        private final BinanceKlineInterval interval;
        private final BinanceKlineSeries series;
        private final ArrayDeque<Update> pending = new ArrayDeque<>();
        private final StringBuilder fragments = new StringBuilder();
        private WebSocket socket;
        private boolean awaitingSnapshot;
        private long generation;
        private long lastMessageNanos;

        private Stream(String symbol, BinanceKlineInterval interval) { this.symbol = symbol; this.interval = interval; this.series = new BinanceKlineSeries(interval); }

        private synchronized void connect() {
            if (closed || socket != null || awaitingSnapshot) return;
            awaitingSnapshot = true;
            long current = ++generation;
            URI uri = URI.create("wss://stream.binance.com:9443/ws/" + symbol.toLowerCase(Locale.ROOT) + "@kline_" + interval.value());
            WebSocket.Listener listener = new WebSocket.Listener() {
                @Override public void onOpen(WebSocket ws) { Stream.this.onOpen(current, ws); }
                @Override public CompletionStage<?> onText(WebSocket ws, CharSequence text, boolean last) { return Stream.this.onText(ws, text, last); }
                @Override public CompletionStage<?> onClose(WebSocket ws, int status, String reason) { return Stream.this.onClose(ws); }
                @Override public void onError(WebSocket ws, Throwable error) { Stream.this.onError(ws, error); }
            };
            client.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5)).buildAsync(uri, listener)
                    .whenComplete((ws, failure) -> { if (failure != null) { warn("connect", failure); fail(current); } });
        }

        private synchronized void onOpen(long current, WebSocket webSocket) {
            if (closed || current != generation || socket != null || !awaitingSnapshot) { webSocket.abort(); return; }
            socket = webSocket;
            lastMessageNanos = System.nanoTime();
            webSocket.request(1);
            Instant requestedAt = Instant.now();
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://data-api.binance.vision/api/v3/klines?symbol=" + symbol + "&interval=" + interval.value() + "&limit=320"))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).whenComplete((response, failure) -> {
                if (failure != null) { warn("history", failure); fail(current); return; }
                try (var body = response.body()) {
                    if (response.statusCode() != 200) { LOG.warn("Binance kline history rejected: symbol={}, status={}", symbol, response.statusCode()); fail(current); return; }
                    byte[] bytes = body.readNBytes(MAX_BYTES + 1);
                    if (bytes.length > MAX_BYTES) { fail(current); return; }
                    acceptSnapshot(current, new String(bytes, StandardCharsets.UTF_8), requestedAt);
                } catch (Exception exception) { warn("history", exception); fail(current); }
            });
        }

        private synchronized CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (webSocket != socket) return null;
            try {
                if (fragments.length() + data.length() > MAX_BYTES) throw new IllegalArgumentException("kline frame too large");
                fragments.append(data);
                if (last) {
                    lastMessageNanos = System.nanoTime();
                    JsonNode node = mapper.readTree(fragments.toString());
                    fragments.setLength(0);
                    JsonNode kline = node.path("k");
                    if (!"kline".equals(node.path("e").asText()) || !symbol.equals(node.path("s").asText())
                            || !symbol.equals(kline.path("s").asText()) || !interval.value().equals(kline.path("i").asText())
                            || !kline.path("x").isBoolean()) throw new IllegalArgumentException("unexpected kline stream");
                    if (number(kline, "T") != interval.nextOpen(number(kline, "t")) - 1) throw new IllegalArgumentException("invalid event close time");
                    Instant arrival = Instant.now();
                    Update update = new Update(new BinanceKlineSeries.Candle(number(kline, "t"), text(kline, "o"), text(kline, "h"), text(kline, "l"),
                            text(kline, "c"), text(kline, "v"), number(kline, "n"), kline.path("x").booleanValue()),
                            Instant.ofEpochMilli(number(node, "E")), arrival);
                    if (awaitingSnapshot) {
                        if (pending.size() >= 256) throw new IllegalStateException("kline buffer overflow");
                        pending.add(update);
                    } else series.update(update.candle(), update.sourceAt(), update.receivedAt());
                }
                webSocket.request(1);
            } catch (Exception exception) { warn("event", exception); fail(generation); }
            return null;
        }

        private synchronized void acceptSnapshot(long current, String body, Instant requestedAt) {
            if (closed || current != generation || socket == null) return;
            try {
                JsonNode data = mapper.readTree(body);
                if (!data.isArray() || data.size() < 2 || data.size() > 320) throw new IllegalArgumentException("invalid history");
                List<BinanceKlineSeries.Candle> history = new ArrayList<>();
                for (JsonNode row : data) {
                    if (!row.isArray() || row.size() < 9) throw new IllegalArgumentException("invalid history row");
                    long openTime = number(row, 0);
                    long closeTime = number(row, 6);
                    if (closeTime != interval.nextOpen(openTime) - 1) throw new IllegalArgumentException("invalid close time");
                    history.add(new BinanceKlineSeries.Candle(openTime, text(row, 1), text(row, 2), text(row, 3), text(row, 4),
                            text(row, 5), number(row, 8), closeTime < requestedAt.toEpochMilli()));
                }
                series.snapshot(history);
                while (!pending.isEmpty()) {
                    Update update = pending.remove();
                    series.update(update.candle(), update.sourceAt(), update.receivedAt());
                }
                awaitingSnapshot = false;
            } catch (Exception exception) { warn("resync", exception); fail(current); }
        }

        private synchronized CompletionStage<?> onClose(WebSocket ws) { if (ws == socket) fail(generation); return null; }
        private synchronized void onError(WebSocket ws, Throwable failure) { if (ws == socket) { warn("websocket", failure); fail(generation); } }
        private synchronized void checkLiveness() { if (socket != null && System.nanoTime() - lastMessageNanos > TimeUnit.SECONDS.toNanos(15)) fail(generation); }
        private void warn(String stage, Throwable failure) {
            Throwable cause = failure.getCause() == null ? failure : failure.getCause();
            LOG.warn("Binance kline unavailable: symbol={}, interval={}, stage={}, errorType={}", symbol, interval.value(), stage, cause.getClass().getSimpleName());
        }
        private synchronized void fail(long current) {
            if (current != generation || closed) return;
            generation++;
            series.invalidate();
            pending.clear();
            fragments.setLength(0);
            awaitingSnapshot = false;
            if (socket != null) { socket.abort(); socket = null; }
            scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
        }
        private synchronized void stop() { generation++; series.invalidate(); if (socket != null) socket.abort(); socket = null; }
    }

    private static long number(JsonNode node, String field) { return number(node == null ? null : node.get(field)); }
    private static long number(JsonNode node, int index) { return number(node == null ? null : node.get(index)); }
    private static long number(JsonNode value) {
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) throw new IllegalArgumentException("invalid integer");
        return value.longValue();
    }
    private static String text(JsonNode node, String field) { return text(node == null ? null : node.get(field)); }
    private static String text(JsonNode node, int index) { return text(node == null ? null : node.get(index)); }
    private static String text(JsonNode value) {
        if (value == null || !value.isTextual()) throw new IllegalArgumentException("invalid decimal field");
        return value.textValue();
    }
}
