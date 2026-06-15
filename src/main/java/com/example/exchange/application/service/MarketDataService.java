/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.DepthDelta;
import com.example.exchange.domain.model.dto.MarketKline;
import com.example.exchange.domain.model.dto.MarketDataRecoveryCursor;
import com.example.exchange.domain.model.dto.MarketTicker;
import com.example.exchange.domain.model.dto.PriceLevel;
import com.example.exchange.domain.model.dto.TopOfBook;
import com.example.exchange.domain.model.dto.TradeTapeItem;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.repository.MarketDataDepthDeltaStore;
import com.example.exchange.domain.repository.MarketDataKlineStore;
import com.example.exchange.domain.repository.MarketDataTickerStore;
import com.example.exchange.domain.repository.MarketDataTradeTapeStore;
import com.example.exchange.domain.service.OrderBookSnapshot;
import com.example.exchange.domain.util.OrderBookChecksum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MarketDataService {

    private static final int MAX_TAPE_SIZE = 500;

    private final Map<String, OrderBookSnapshot> previousDepth = new ConcurrentHashMap<>();
    private final Map<String, DepthDelta> latestDepthDelta = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> depthVersions = new ConcurrentHashMap<>();
    private final Map<String, Deque<TradeTapeItem>> tradeTapes = new ConcurrentHashMap<>();
    private final Map<String, MarketTicker> tickers = new ConcurrentHashMap<>();
    private final Map<String, Map<Instant, MarketKline>> oneMinuteKlines = new ConcurrentHashMap<>();
    private PushGatewayService pushGatewayService;
    private MarketDataSequenceCheckpointService sequenceCheckpointService;
    private MarketDataDepthDeltaStore depthDeltaStore;
    private MarketDataKlineStore klineStore;
    private MarketDataTradeTapeStore tradeTapeStore;
    private MarketDataTickerStore tickerStore;

    @Autowired(required = false)
    public void setPushGatewayService(PushGatewayService pushGatewayService) {
        this.pushGatewayService = pushGatewayService;
    }

    @Autowired(required = false)
    public void setSequenceCheckpointService(MarketDataSequenceCheckpointService sequenceCheckpointService) {
        this.sequenceCheckpointService = sequenceCheckpointService;
    }

    @Autowired(required = false)
    public void setDepthDeltaStore(MarketDataDepthDeltaStore depthDeltaStore) {
        this.depthDeltaStore = depthDeltaStore;
    }

    @Autowired(required = false)
    public void setKlineStore(MarketDataKlineStore klineStore) {
        this.klineStore = klineStore;
    }

    @Autowired(required = false)
    public void setTradeTapeStore(MarketDataTradeTapeStore tradeTapeStore) {
        this.tradeTapeStore = tradeTapeStore;
    }

    @Autowired(required = false)
    public void setTickerStore(MarketDataTickerStore tickerStore) {
        this.tickerStore = tickerStore;
    }

    public void onOrderBookChanged(String symbol, OrderBookSnapshot snapshot, Optional<TopOfBook> top) {
        String code = normalize(symbol);
        DepthDelta delta = computeDepthDelta(code, previousDepth.put(code, snapshot), snapshot);
        latestDepthDelta.put(code, delta);
        updateTop(code, top);
        if (pushGatewayService != null) {
            pushGatewayService.publishMarket(code, "depth-delta", delta);
            ticker(code).ifPresent(ticker -> pushGatewayService.publishMarket(code, "ticker", ticker));
        }
    }

    public void onTrades(
            String symbol,
            List<TradeExecuted> trades,
            OrderBookSnapshot snapshot,
            Optional<TopOfBook> top
    ) {
        String code = normalize(symbol);
        if (trades != null) {
            trades.stream()
                    .filter(trade -> !trade.maker())
                    .forEach(trade -> appendTrade(code, trade));
        }
        onOrderBookChanged(code, snapshot, top);
    }

    public Optional<DepthDelta> latestDepthDelta(String symbol) {
        return Optional.ofNullable(latestDepthDelta.get(normalize(symbol)));
    }

    public List<DepthDelta> depthDeltasAfter(String symbol, long afterVersion, int limit) {
        String code = normalize(symbol);
        int normalizedLimit = Math.max(1, limit);
        if (depthDeltaStore != null) {
            return depthDeltaStore.findAfter(code, afterVersion, normalizedLimit);
        }
        return latestDepthDelta(code)
                .filter(delta -> delta.version() > afterVersion)
                .stream()
                .limit(normalizedLimit)
                .toList();
    }

    public long depthVersion(String symbol) {
        String code = normalize(symbol);
        AtomicLong version = depthVersions.computeIfAbsent(code, this::initialDepthVersion);
        return version == null ? 0L : version.get();
    }

    public Optional<MarketTicker> ticker(String symbol) {
        String code = normalize(symbol);
        MarketTicker current = tickers.get(code);
        if (current != null) return Optional.of(current);
        if (tickerStore != null) {
            return tickerStore.find(code);
        }
        return Optional.empty();
    }

    public List<TradeTapeItem> trades(String symbol, int limit) {
        if (tradeTapeStore != null) {
            return tradeTapeStore.findRecent(normalize(symbol), limit);
        }
        Deque<TradeTapeItem> tape = tradeTapes.get(normalize(symbol));
        if (tape == null) return List.of();
        synchronized (tape) {
            return tape.stream()
                    .sorted(Comparator.comparing(TradeTapeItem::ts).reversed())
                    .limit(Math.max(1, limit))
                    .toList();
        }
    }

    public List<TradeTapeItem> tradesAfter(String symbol, Instant afterTs, String afterMatchId, int limit) {
        String code = normalize(symbol);
        int normalizedLimit = Math.max(1, limit);
        if (afterTs == null) {
            return trades(code, normalizedLimit);
        }
        if (tradeTapeStore != null) {
            return tradeTapeStore.findAfter(code, afterTs, afterMatchId, normalizedLimit);
        }
        Deque<TradeTapeItem> tape = tradeTapes.get(code);
        if (tape == null) return List.of();
        String normalizedMatchId = afterMatchId == null ? null : afterMatchId.trim();
        synchronized (tape) {
            return tape.stream()
                    .filter(item -> isAfterCursor(item, afterTs, normalizedMatchId))
                    .sorted(Comparator.comparing(TradeTapeItem::ts)
                            .thenComparing(TradeTapeItem::matchId))
                    .limit(normalizedLimit)
                    .toList();
        }
    }

    public List<TradeTapeItem> tradesBefore(String symbol, Instant beforeTs, String beforeMatchId, int limit) {
        String code = normalize(symbol);
        int normalizedLimit = Math.max(1, limit);
        if (beforeTs == null) {
            return trades(code, normalizedLimit);
        }
        if (tradeTapeStore != null) {
            return tradeTapeStore.findBefore(code, beforeTs, beforeMatchId, normalizedLimit);
        }
        Deque<TradeTapeItem> tape = tradeTapes.get(code);
        if (tape == null) {
            return List.of();
        }
        String normalizedMatchId = beforeMatchId == null ? null : beforeMatchId.trim();
        synchronized (tape) {
            return tape.stream()
                    .filter(item -> isBeforeCursor(item, beforeTs, normalizedMatchId))
                    .sorted(Comparator.comparing(TradeTapeItem::ts).reversed()
                            .thenComparing(TradeTapeItem::matchId).reversed())
                    .limit(normalizedLimit)
                    .toList();
        }
    }

    public MarketDataRecoveryCursor recoveryCursor(String symbol) {
        String code = normalize(symbol);
        TradeTapeItem latestTrade = trades(code, 1).stream().findFirst().orElse(null);
        return new MarketDataRecoveryCursor(
                code,
                depthVersion(code),
                latestTrade == null ? null : latestTrade.ts(),
                latestTrade == null ? null : latestTrade.matchId(),
                Instant.now()
        );
    }

    public List<MarketKline> klines(String symbol, int limit) {
        if (klineStore != null) {
            return klineStore.findRecent(normalize(symbol), "1m", limit);
        }
        Map<Instant, MarketKline> klines = oneMinuteKlines.get(normalize(symbol));
        if (klines == null) return List.of();
        return klines.values().stream()
                .sorted(Comparator.comparing(MarketKline::openTime).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    private void appendTrade(String symbol, TradeExecuted trade) {
        TradeTapeItem item = new TradeTapeItem(
                symbol,
                trade.matchId(),
                trade.orderId(),
                trade.qty().signum() >= 0 ? OrderSide.BUY : OrderSide.SELL,
                trade.price(),
                trade.absQty(),
                trade.maker(),
                trade.ts()
        );

        Deque<TradeTapeItem> tape = tradeTapes.computeIfAbsent(symbol, ignored -> new ArrayDeque<>());
        synchronized (tape) {
            tape.addLast(item);
            while (tape.size() > MAX_TAPE_SIZE) tape.removeFirst();
        }
        if (tradeTapeStore != null) {
            tradeTapeStore.append(item);
        }

        updateTickerFromTrade(symbol, trade);
        updateKline(symbol, trade);
        if (pushGatewayService != null) {
            pushGatewayService.publishMarket(symbol, "trade", item);
        }
    }

    private void updateTickerFromTrade(String symbol, TradeExecuted trade) {
        MarketTicker updated = tickers.compute(symbol, (ignored, current) -> {
            BigDecimal volume = (current == null ? BigDecimal.ZERO : current.volume24h()).add(trade.absQty());
            BigDecimal high = current == null || current.high24h() == null
                    ? trade.price()
                    : current.high24h().max(trade.price());
            BigDecimal low = current == null || current.low24h() == null
                    ? trade.price()
                    : current.low24h().min(trade.price());
            return new MarketTicker(
                    symbol,
                    trade.price(),
                    current == null ? null : current.bestBid(),
                    current == null ? null : current.bestAsk(),
                    volume,
                    high,
                    low,
                    trade.ts()
            );
        });
        saveTicker(updated);
    }

    private void updateTop(String symbol, Optional<TopOfBook> top) {
        BigDecimal bestBid = top.map(TopOfBook::getBestBid).orElse(null);
        BigDecimal bestAsk = top.map(TopOfBook::getBestAsk).orElse(null);
        MarketTicker updated = tickers.compute(symbol, (ignored, current) -> new MarketTicker(
                symbol,
                current == null ? null : current.lastPrice(),
                bestBid,
                bestAsk,
                current == null ? BigDecimal.ZERO : current.volume24h(),
                current == null ? null : current.high24h(),
                current == null ? null : current.low24h(),
                Instant.now()
        ));
        saveTicker(updated);
    }

    private void saveTicker(MarketTicker ticker) {
        if (tickerStore != null && ticker != null) {
            tickerStore.save(ticker);
        }
    }

    private void updateKline(String symbol, TradeExecuted trade) {
        Instant openTime = trade.ts().truncatedTo(ChronoUnit.MINUTES);
        MarketKline updated = oneMinuteKlines.computeIfAbsent(symbol, ignored -> new ConcurrentHashMap<>())
                .compute(openTime, (ignored, current) -> {
                    if (current == null) {
                        return new MarketKline(
                                symbol,
                                "1m",
                                openTime,
                                trade.price(),
                                trade.price(),
                                trade.price(),
                                trade.price(),
                                trade.absQty()
                        );
                    }
                    return new MarketKline(
                            symbol,
                            "1m",
                            openTime,
                            current.open(),
                            current.high().max(trade.price()),
                            current.low().min(trade.price()),
                            trade.price(),
                            current.volume().add(trade.absQty())
                    );
                });
        if (klineStore != null) {
            klineStore.save(updated);
        }
    }

    private DepthDelta computeDepthDelta(String symbol, OrderBookSnapshot previous, OrderBookSnapshot current) {
        List<PriceLevel> bidDelta = diff(previous == null ? List.of() : previous.bids(), current.bids());
        List<PriceLevel> askDelta = diff(previous == null ? List.of() : previous.asks(), current.asks());
        long version = depthVersions.computeIfAbsent(symbol, this::initialDepthVersion).incrementAndGet();
        long checksum = OrderBookChecksum.crc32(current.bids(), current.asks());
        Instant now = Instant.now();
        if (sequenceCheckpointService != null) {
            sequenceCheckpointService.advance(
                    symbol,
                    MarketDataSequenceCheckpointService.DEPTH_DELTA_STREAM,
                    version,
                    checksum,
                    now
            );
        }
        DepthDelta delta = new DepthDelta(symbol, version, checksum, bidDelta, askDelta, now);
        if (depthDeltaStore != null) {
            depthDeltaStore.append(delta);
        }
        return delta;
    }

    private AtomicLong initialDepthVersion(String symbol) {
        long latest = sequenceCheckpointService == null
                ? 0L
                : sequenceCheckpointService.latestSequence(
                        symbol,
                        MarketDataSequenceCheckpointService.DEPTH_DELTA_STREAM
                );
        return new AtomicLong(latest);
    }

    private static List<PriceLevel> diff(List<PriceLevel> previous, List<PriceLevel> current) {
        Map<BigDecimal, PriceLevel> prevMap = previous.stream()
                .collect(Collectors.toMap(PriceLevel::price, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<BigDecimal, PriceLevel> curMap = current.stream()
                .collect(Collectors.toMap(PriceLevel::price, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        List<PriceLevel> delta = new ArrayList<>();
        for (PriceLevel level : current) {
            PriceLevel old = prevMap.get(level.price());
            if (old == null || old.qty().compareTo(level.qty()) != 0) {
                delta.add(level);
            }
        }
        for (PriceLevel old : previous) {
            if (!curMap.containsKey(old.price())) {
                delta.add(new PriceLevel(old.price(), BigDecimal.ZERO));
            }
        }
        return delta;
    }

    private static boolean isAfterCursor(TradeTapeItem item, Instant afterTs, String afterMatchId) {
        if (item.ts().isAfter(afterTs)) {
            return true;
        }
        if (!item.ts().equals(afterTs)) {
            return false;
        }
        return afterMatchId == null || item.matchId().compareTo(afterMatchId) > 0;
    }

    private static boolean isBeforeCursor(TradeTapeItem item, Instant beforeTs, String beforeMatchId) {
        if (item.ts().isBefore(beforeTs)) {
            return true;
        }
        if (!item.ts().equals(beforeTs)) {
            return false;
        }
        return beforeMatchId == null || item.matchId().compareTo(beforeMatchId) < 0;
    }

    private static String normalize(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }
}
