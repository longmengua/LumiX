/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.DepthDelta;
import com.example.exchange.domain.model.dto.MarketKline;
import com.example.exchange.domain.model.dto.MarketTicker;
import com.example.exchange.domain.model.dto.PriceLevel;
import com.example.exchange.domain.model.dto.TopOfBook;
import com.example.exchange.domain.model.dto.TradeTapeItem;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.util.OrderBookChecksum;
import com.example.exchange.domain.service.OrderBookSnapshot;
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

    @Autowired(required = false)
    public void setPushGatewayService(PushGatewayService pushGatewayService) {
        this.pushGatewayService = pushGatewayService;
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

    public long depthVersion(String symbol) {
        AtomicLong version = depthVersions.get(normalize(symbol));
        return version == null ? 0L : version.get();
    }

    public Optional<MarketTicker> ticker(String symbol) {
        return Optional.ofNullable(tickers.get(normalize(symbol)));
    }

    public List<TradeTapeItem> trades(String symbol, int limit) {
        Deque<TradeTapeItem> tape = tradeTapes.get(normalize(symbol));
        if (tape == null) return List.of();
        synchronized (tape) {
            return tape.stream()
                    .sorted(Comparator.comparing(TradeTapeItem::ts).reversed())
                    .limit(Math.max(1, limit))
                    .toList();
        }
    }

    public List<MarketKline> klines(String symbol, int limit) {
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

        updateTickerFromTrade(symbol, trade);
        updateKline(symbol, trade);
        if (pushGatewayService != null) {
            pushGatewayService.publishMarket(symbol, "trade", item);
        }
    }

    private void updateTickerFromTrade(String symbol, TradeExecuted trade) {
        tickers.compute(symbol, (ignored, current) -> {
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
    }

    private void updateTop(String symbol, Optional<TopOfBook> top) {
        BigDecimal bestBid = top.map(TopOfBook::getBestBid).orElse(null);
        BigDecimal bestAsk = top.map(TopOfBook::getBestAsk).orElse(null);
        tickers.compute(symbol, (ignored, current) -> new MarketTicker(
                symbol,
                current == null ? null : current.lastPrice(),
                bestBid,
                bestAsk,
                current == null ? BigDecimal.ZERO : current.volume24h(),
                current == null ? null : current.high24h(),
                current == null ? null : current.low24h(),
                Instant.now()
        ));
    }

    private void updateKline(String symbol, TradeExecuted trade) {
        Instant openTime = trade.ts().truncatedTo(ChronoUnit.MINUTES);
        oneMinuteKlines.computeIfAbsent(symbol, ignored -> new ConcurrentHashMap<>())
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
    }

    private DepthDelta computeDepthDelta(String symbol, OrderBookSnapshot previous, OrderBookSnapshot current) {
        List<PriceLevel> bidDelta = diff(previous == null ? List.of() : previous.bids(), current.bids());
        List<PriceLevel> askDelta = diff(previous == null ? List.of() : previous.asks(), current.asks());
        long version = depthVersions.computeIfAbsent(symbol, ignored -> new AtomicLong()).incrementAndGet();
        long checksum = OrderBookChecksum.crc32(current.bids(), current.asks());
        return new DepthDelta(symbol, version, checksum, bidDelta, askDelta, Instant.now());
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

    private static String normalize(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }
}
