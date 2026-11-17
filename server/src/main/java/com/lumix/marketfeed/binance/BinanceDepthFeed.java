package com.lumix.marketfeed.binance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
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

/**
 * 只接收 Binance 公開現貨深度，使用系統 TLS 信任庫與主機驗證。
 * 預設不啟動；每個交易對獨立快照、緩衝與重連，絕不寫入交易或資產狀態。
 */
@Component
@Profile("infrastructure")
@ConditionalOnProperty(prefix = "lumix.market-feed.binance", name = "enabled", havingValue = "true")
public class BinanceDepthFeed {
    private static final int BUFFER_LIMIT = 2048;
    private static final int FRAME_LIMIT = 1_000_000;
    private static final Logger LOG = LoggerFactory.getLogger(BinanceDepthFeed.class);
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, Stream> streams = new ConcurrentHashMap<>();
    private volatile boolean closed;

    /** 僅固定啟用已指定的兩個交易對，避免由使用者輸入控制上游連線。 */
    public BinanceDepthFeed(ObjectMapper mapper) {
        this.mapper = mapper;
        for (String symbol : List.of("BTCUSDT", "ETHUSDT")) {
            Stream stream = new Stream(symbol);
            streams.put(symbol, stream);
            scheduler.execute(stream::connect);
            scheduler.scheduleWithFixedDelay(stream::checkLiveness, 5, 5, TimeUnit.SECONDS);
        }
    }

    /** 提供來源時間與本機接收時間，讓下游辨識延遲。 */
    public record FeedView(boolean healthy, long sequence, Instant sourceAt, Instant receivedAt,
                           List<BinanceOrderBook.Level> bids, List<BinanceOrderBook.Level> asks) { }

    /** 唯讀快照不含交易資訊；過期行情由 book 隱藏。 */
    public FeedView view(String symbol) {
        Stream stream = streams.get(symbol);
        if (stream == null) throw new IllegalArgumentException("unsupported symbol");
        return stream.view();
    }

    /** 關閉時不可重新排程，並主動中止仍存活的連線。 */
    @PreDestroy
    public void close() {
        closed = true;
        streams.values().forEach(Stream::stop);
        scheduler.shutdownNow();
    }

    private record Update(long first, long last, List<BinanceOrderBook.Level> bids, List<BinanceOrderBook.Level> asks, Instant sourceAt, Instant receivedAt) { }

    private final class Stream {
        private final String symbol;
        private final BinanceOrderBook book = new BinanceOrderBook();
        private final ArrayDeque<Update> pending = new ArrayDeque<>();
        private final StringBuilder fragments = new StringBuilder();
        private WebSocket socket;
        private boolean awaitingSnapshot;
        private long generation;
        private long lastMessageNanos;
        private java.math.BigDecimal snapshotBidFloor;
        private java.math.BigDecimal snapshotAskCeiling;
        private Instant sourceAt;

        private Stream(String symbol) { this.symbol = symbol; }

        private synchronized void connect() {
            if (closed || socket != null || awaitingSnapshot) return;
            awaitingSnapshot = true;
            long current = ++generation;
            URI uri = URI.create("wss://stream.binance.com:9443/ws/" + symbol.toLowerCase(java.util.Locale.ROOT) + "@depth@100ms");
            WebSocket.Listener listener = new WebSocket.Listener() {
                @Override public void onOpen(WebSocket socket) { Stream.this.onOpen(current, socket); }
                @Override public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) { return Stream.this.onText(socket, data, last); }
                @Override public CompletionStage<?> onClose(WebSocket socket, int status, String reason) { return Stream.this.onClose(socket, status, reason); }
                @Override public void onError(WebSocket socket, Throwable error) { Stream.this.onError(socket, error); }
            };
            client.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5)).buildAsync(uri, listener)
                    .whenComplete((ws, failure) -> { if (failure != null) { warn("connect", failure); fail(current); } });
        }

        private synchronized void onOpen(long current, WebSocket webSocket) {
            if (closed || current != generation || !awaitingSnapshot || socket != null) { webSocket.abort(); return; }
            socket = webSocket;
            lastMessageNanos = System.nanoTime();
            webSocket.request(1);
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://data-api.binance.vision/api/v3/depth?symbol=" + symbol + "&limit=1000"))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).whenComplete((response, failure) -> {
                if (failure != null) { warn("snapshot", failure); fail(current); return; }
                try (var body = response.body()) {
                    if (response.statusCode() != 200) { LOG.warn("Binance depth snapshot rejected: symbol={}, status={}", symbol, response.statusCode()); fail(current); return; }
                    byte[] bytes = body.readNBytes(FRAME_LIMIT + 1);
                    if (bytes.length > FRAME_LIMIT) { fail(current); return; }
                    acceptSnapshot(current, new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
                } catch (Exception exception) { warn("snapshot", exception); fail(current); }
            });
        }

        private synchronized CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (webSocket != socket) return null;
            try {
                if (fragments.length() + data.length() > FRAME_LIMIT) throw new IllegalArgumentException("frame too large");
                fragments.append(data);
                if (last) {
                    lastMessageNanos = System.nanoTime();
                    JsonNode node = mapper.readTree(fragments.toString());
                    fragments.setLength(0);
                    if (!symbol.equals(node.path("s").asText()) || !"depthUpdate".equals(node.path("e").asText())) throw new IllegalArgumentException("unexpected stream");
                    Instant receivedAt = Instant.now();
                    Instant sourceTime = Instant.ofEpochMilli(requiredLong(node, "E"));
                    if (sourceTime.isBefore(receivedAt.minusSeconds(10)) || sourceTime.isAfter(receivedAt.plusSeconds(5))) throw new IllegalArgumentException("source time outside allowed window");
                    Update update = new Update(requiredLong(node, "U"), requiredLong(node, "u"), levels(node.get("b")), levels(node.get("a")), sourceTime, receivedAt);
                    if (awaitingSnapshot) {
                        if (pending.size() >= BUFFER_LIMIT) throw new IllegalStateException("snapshot buffer overflow");
                        pending.add(update);
                    } else if (apply(update) == BinanceDepthSequence.Decision.RESYNC_REQUIRED) {
                        throw new IllegalStateException("depth sequence gap");
                    }
                }
                webSocket.request(1);
            } catch (Exception exception) { warn("depth", exception); fail(generation); }
            return null;
        }

        private synchronized void acceptSnapshot(long current, String body) {
            if (closed || current != generation || socket == null) return;
            try {
                JsonNode snapshot = mapper.readTree(body);
                // snapshot 的時間戳並非來源事件時間；只記錄本機接收時間供 freshness 判斷。
                List<BinanceOrderBook.Level> snapshotBids = levels(snapshot.get("bids"));
                List<BinanceOrderBook.Level> snapshotAsks = levels(snapshot.get("asks"));
                book.snapshot(requiredLong(snapshot, "lastUpdateId"), snapshotBids, snapshotAsks, Instant.now());
                snapshotBidFloor = snapshotBids.stream().map(level -> new java.math.BigDecimal(level.price())).min(java.math.BigDecimal::compareTo).orElseThrow();
                snapshotAskCeiling = snapshotAsks.stream().map(level -> new java.math.BigDecimal(level.price())).max(java.math.BigDecimal::compareTo).orElseThrow();
                while (!pending.isEmpty()) {
                    Update update = pending.remove();
                    // 緩衝增量在快照接收前到達，依序套用但使用不早於快照的本機時間。
                    Update buffered = new Update(update.first(), update.last(), update.bids(), update.asks(), update.sourceAt(), Instant.now());
                    if (apply(buffered) == BinanceDepthSequence.Decision.RESYNC_REQUIRED) throw new IllegalStateException("snapshot gap");
                }
                awaitingSnapshot = false;
            } catch (Exception exception) { warn("resync", exception); fail(current); }
        }

        private BinanceDepthSequence.Decision apply(Update update) {
            BinanceDepthSequence.Decision decision = book.delta(update.first(), update.last(), update.bids(), update.asks(), update.receivedAt());
            if (decision == BinanceDepthSequence.Decision.APPLY) sourceAt = update.sourceAt();
            return decision;
        }

        private synchronized FeedView view() {
            BinanceOrderBook.View view = book.view(Instant.now(), Duration.ofSeconds(5));
            if (!view.healthy()) return new FeedView(false, view.sequence(), sourceAt, view.receivedAt(), List.of(), List.of());
            // REST 快照只有有限深度；前 20 檔超出來源快照邊界時，未知舊價位可能被漏掉。
            boolean complete = snapshotBidFloor != null && snapshotAskCeiling != null
                    && view.bids().size() >= 20 && view.asks().size() >= 20
                    && new java.math.BigDecimal(view.bids().get(19).price()).compareTo(snapshotBidFloor) >= 0
                    && new java.math.BigDecimal(view.asks().get(19).price()).compareTo(snapshotAskCeiling) <= 0
                    && sourceAt != null && !sourceAt.isBefore(Instant.now().minusSeconds(10));
            if (!complete) {
                fail(generation);
                return new FeedView(false, view.sequence(), null, null, List.of(), List.of());
            }
            // 限制每個訂閱者的單筆負載，並保持來源數值的十進位字串。
            return new FeedView(true, view.sequence(), sourceAt, view.receivedAt(), view.bids().subList(0, 20), view.asks().subList(0, 20));
        }

        private synchronized void checkLiveness() {
            if (socket != null && System.nanoTime() - lastMessageNanos > TimeUnit.SECONDS.toNanos(15)) fail(generation);
        }

        private synchronized CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (webSocket == socket) fail(generation);
            return null;
        }
        private synchronized void onError(WebSocket webSocket, Throwable error) { if (webSocket == socket) { warn("websocket", error); fail(generation); } }

        private void warn(String stage, Throwable failure) {
            // 只輸出錯誤類型，避免 provider 訊息中的 URL、標頭或其他敏感資料進入日誌。
            Throwable cause = failure.getCause() == null ? failure : failure.getCause();
            LOG.warn("Binance depth feed unavailable: symbol={}, stage={}, errorType={}", symbol, stage, cause.getClass().getSimpleName());
        }

        private synchronized void fail(long current) {
            if (current != generation || closed) return;
            generation++;
            book.invalidate();
            pending.clear();
            snapshotBidFloor = null;
            snapshotAskCeiling = null;
            sourceAt = null;
            fragments.setLength(0);
            awaitingSnapshot = false;
            if (socket != null) { socket.abort(); socket = null; }
            // 固定延遲避免來源故障時形成無界重連風暴。
            scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
        }
        private synchronized void stop() { generation++; book.invalidate(); if (socket != null) socket.abort(); socket = null; }
    }

    private static long requiredLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) throw new IllegalArgumentException("invalid sequence");
        return value.longValue();
    }

    private static List<BinanceOrderBook.Level> levels(JsonNode values) {
        if (values == null || !values.isArray() || values.size() > 10000) throw new IllegalArgumentException("invalid levels");
        List<BinanceOrderBook.Level> result = new ArrayList<>(values.size());
        for (JsonNode pair : values) {
            if (!pair.isArray() || pair.size() != 2 || !pair.get(0).isTextual() || !pair.get(1).isTextual()) throw new IllegalArgumentException("invalid level");
            result.add(new BinanceOrderBook.Level(pair.get(0).textValue(), pair.get(1).textValue()));
        }
        return List.copyOf(result);
    }
}
