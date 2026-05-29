/*
 * 檔案用途：測試 market-data durable sequence checkpoint 與 depth recovery baseline。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.DepthDelta;
import com.example.exchange.domain.model.dto.MarketKline;
import com.example.exchange.domain.model.dto.MarketTicker;
import com.example.exchange.domain.model.dto.MarketDataSequenceCheckpoint;
import com.example.exchange.domain.model.dto.PriceLevel;
import com.example.exchange.domain.model.dto.TradeTapeItem;
import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.repository.MarketDataDepthDeltaStore;
import com.example.exchange.domain.repository.MarketDataKlineStore;
import com.example.exchange.domain.repository.MarketDataSequenceCheckpointStore;
import com.example.exchange.domain.repository.MarketDataTickerStore;
import com.example.exchange.domain.repository.MarketDataTradeTapeStore;
import com.example.exchange.domain.service.OrderBookSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MarketDataSequenceCheckpointServiceTest {

    @Test
    @DisplayName("depth checkpoint 只接受單調遞增 sequence")
    /**
     * 流程：先寫 sequence=10，再重送 duplicate=10 與 out-of-order=9。
     * 期望：checkpoint 停在 10，避免 reconnect backfill 因舊 delta 倒退。
     */
    void checkpointIgnoresDuplicateAndOutOfOrderSequences() {
        MemMarketDataSequenceCheckpointStore store = new MemMarketDataSequenceCheckpointStore();
        MarketDataSequenceCheckpointService service = new MarketDataSequenceCheckpointService(store);

        MarketDataSequenceCheckpoint first = service.advance("btcusdt", "depth_delta", 10, 1001, Instant.EPOCH);
        MarketDataSequenceCheckpoint duplicate = service.advance("BTCUSDT", "DEPTH_DELTA", 10, 9999, Instant.EPOCH.plusSeconds(1));
        MarketDataSequenceCheckpoint outOfOrder = service.advance("BTCUSDT", "DEPTH_DELTA", 9, 8888, Instant.EPOCH.plusSeconds(2));

        assertThat(first.sequence()).isEqualTo(10);
        assertThat(duplicate.sequence()).isEqualTo(10);
        assertThat(outOfOrder.sequence()).isEqualTo(10);
        assertThat(service.latestSequence("BTCUSDT", "DEPTH_DELTA")).isEqualTo(10);
        assertThat(store.saveCount).isEqualTo(1);
    }

    @Test
    @DisplayName("market data service 會從 durable checkpoint 恢復 depth version")
    /**
     * 流程：durable checkpoint 已保存 sequence=7 -> 新 MarketDataService 啟動後先查 version -> 新 delta 從 8 開始。
     * 這固定 snapshot-plus-delta 的 reconnect 假設：client 拿 snapshot 後，只需 backfill 大於 checkpoint 的 delta。
     */
    void marketDataServiceRestoresDepthVersionFromCheckpoint() {
        MemMarketDataSequenceCheckpointStore store = new MemMarketDataSequenceCheckpointStore();
        MarketDataSequenceCheckpointService checkpointService = new MarketDataSequenceCheckpointService(store);
        checkpointService.advance("BTCUSDT", "DEPTH_DELTA", 7, 700, Instant.EPOCH);
        MarketDataService marketDataService = new MarketDataService();
        marketDataService.setSequenceCheckpointService(checkpointService);

        assertThat(marketDataService.depthVersion("BTCUSDT")).isEqualTo(7);

        marketDataService.onOrderBookChanged("BTCUSDT", snapshot("100", "1"), Optional.empty());

        assertThat(marketDataService.latestDepthDelta("BTCUSDT")).hasValueSatisfying(delta -> {
            assertThat(delta.version()).isEqualTo(8);
            assertThat(delta.checksum()).isNotZero();
        });
        assertThat(checkpointService.latestSequence("BTCUSDT", "DEPTH_DELTA")).isEqualTo(8);
    }

    @Test
    @DisplayName("market data service 可查詢指定 sequence 之後的 depth deltas")
    /**
     * 流程：連續產生兩筆 depth delta -> client 以 afterVersion=1 reconnect。
     * 期望：backfill 只回 sequence=2 的 delta，保留 snapshot-plus-delta 的單調版本語意。
     */
    void marketDataServiceBackfillsDepthDeltasAfterKnownVersion() {
        MemMarketDataDepthDeltaStore deltaStore = new MemMarketDataDepthDeltaStore();
        MarketDataService marketDataService = new MarketDataService();
        marketDataService.setDepthDeltaStore(deltaStore);

        marketDataService.onOrderBookChanged("BTCUSDT", snapshot("100", "1"), Optional.empty());
        marketDataService.onOrderBookChanged("BTCUSDT", snapshot("101", "2"), Optional.empty());

        List<DepthDelta> backfill = marketDataService.depthDeltasAfter("BTCUSDT", 1, 10);

        assertThat(backfill).singleElement()
                .satisfies(delta -> {
                    assertThat(delta.version()).isEqualTo(2);
                    assertThat(delta.bids()).extracting(PriceLevel::price)
                            .containsExactly(new BigDecimal("101"), new BigDecimal("100"));
                });
    }

    @Test
    @DisplayName("market data service 會持久化 trade tape 並可於重啟後查詢")
    /**
     * 流程：第一個 MarketDataService 收到 taker trade 後寫入 durable tape ->
     * 新 service instance 只接同一個 store -> trades API 仍能查到最近成交。
     */
    void marketDataServicePersistsTradeTapeForRestartSafeRecentTrades() {
        MemMarketDataTradeTapeStore tradeTapeStore = new MemMarketDataTradeTapeStore();
        MarketDataService writer = new MarketDataService();
        writer.setTradeTapeStore(tradeTapeStore);

        writer.onTrades(
                "BTCUSDT",
                List.of(trade("match-1", "100", "2")),
                snapshot("100", "1"),
                Optional.empty()
        );
        MarketDataService readerAfterRestart = new MarketDataService();
        readerAfterRestart.setTradeTapeStore(tradeTapeStore);

        assertThat(readerAfterRestart.trades("BTCUSDT", 10)).singleElement()
                .satisfies(item -> {
                    assertThat(item.matchId()).isEqualTo("match-1");
                    assertThat(item.price()).isEqualByComparingTo("100");
                    assertThat(item.qty()).isEqualByComparingTo("2");
                });
    }

    @Test
    @DisplayName("market data service 會持久化 ticker latest state 並可於重啟後查詢")
    /**
     * 流程：第一個 MarketDataService 收到 taker trade 後更新 ticker latest state ->
     * 新 service instance 只接同一個 store -> ticker API 仍能查到最新成交價與 24h 統計。
     */
    void marketDataServicePersistsTickerLatestStateForRestartSafeReads() {
        MemMarketDataTickerStore tickerStore = new MemMarketDataTickerStore();
        MarketDataService writer = new MarketDataService();
        writer.setTickerStore(tickerStore);

        writer.onTrades(
                "BTCUSDT",
                List.of(trade("match-2", "101", "3")),
                snapshot("101", "1"),
                Optional.empty()
        );
        MarketDataService readerAfterRestart = new MarketDataService();
        readerAfterRestart.setTickerStore(tickerStore);

        assertThat(readerAfterRestart.ticker("BTCUSDT")).hasValueSatisfying(ticker -> {
            assertThat(ticker.lastPrice()).isEqualByComparingTo("101");
            assertThat(ticker.volume24h()).isEqualByComparingTo("3");
            assertThat(ticker.high24h()).isEqualByComparingTo("101");
            assertThat(ticker.low24h()).isEqualByComparingTo("101");
        });
    }

    @Test
    @DisplayName("market data service 會持久化 1m kline 並可於重啟後查詢")
    /**
     * 流程：同一分鐘內連續兩筆 taker trade 更新同一根 1m kline ->
     * 新 service instance 只接同一個 store -> klines API 仍能查到 OHLCV 聚合結果。
     */
    void marketDataServicePersistsOneMinuteKlinesForRestartSafeReads() {
        MemMarketDataKlineStore klineStore = new MemMarketDataKlineStore();
        MarketDataService writer = new MarketDataService();
        writer.setKlineStore(klineStore);

        writer.onTrades("BTCUSDT", List.of(trade("match-3", "100", "2")), snapshot("100", "1"), Optional.empty());
        writer.onTrades("BTCUSDT", List.of(trade("match-4", "103", "4")), snapshot("103", "1"), Optional.empty());
        MarketDataService readerAfterRestart = new MarketDataService();
        readerAfterRestart.setKlineStore(klineStore);

        assertThat(readerAfterRestart.klines("BTCUSDT", 10)).singleElement()
                .satisfies(kline -> {
                    assertThat(kline.interval()).isEqualTo("1m");
                    assertThat(kline.open()).isEqualByComparingTo("100");
                    assertThat(kline.high()).isEqualByComparingTo("103");
                    assertThat(kline.low()).isEqualByComparingTo("100");
                    assertThat(kline.close()).isEqualByComparingTo("103");
                    assertThat(kline.volume()).isEqualByComparingTo("6");
                });
    }

    private static OrderBookSnapshot snapshot(String bidPrice, String bidQty) {
        return new OrderBookSnapshot(
                List.of(new PriceLevel(new BigDecimal(bidPrice), new BigDecimal(bidQty))),
                List.of()
        );
    }

    private static TradeExecuted trade(String matchId, String price, String qty) {
        return new TradeExecuted(
                7,
                Symbol.builder().base("BTC").quote("USDT").priceScale(1).qtyScale(3).build(),
                new BigDecimal(qty),
                new BigDecimal(price),
                1,
                Instant.EPOCH,
                UUID.randomUUID(),
                UUID.randomUUID(),
                matchId,
                false
        );
    }

    private static class MemMarketDataSequenceCheckpointStore implements MarketDataSequenceCheckpointStore {
        private final Map<String, MarketDataSequenceCheckpoint> checkpoints = new LinkedHashMap<>();
        private int saveCount;

        @Override
        public Optional<MarketDataSequenceCheckpoint> find(String symbol, String stream) {
            return Optional.ofNullable(checkpoints.get(key(symbol, stream)));
        }

        @Override
        public MarketDataSequenceCheckpoint save(MarketDataSequenceCheckpoint checkpoint) {
            saveCount++;
            checkpoints.put(key(checkpoint.symbol(), checkpoint.stream()), checkpoint);
            return checkpoint;
        }

        private static String key(String symbol, String stream) {
            return symbol + ":" + stream;
        }
    }

    private static class MemMarketDataDepthDeltaStore implements MarketDataDepthDeltaStore {
        private final List<DepthDelta> deltas = new java.util.ArrayList<>();

        @Override
        public void append(DepthDelta delta) {
            deltas.add(delta);
        }

        @Override
        public List<DepthDelta> findAfter(String symbol, long afterVersion, int limit) {
            return deltas.stream()
                    .filter(delta -> delta.symbol().equals(symbol))
                    .filter(delta -> delta.version() > afterVersion)
                    .limit(Math.max(1, limit))
                    .toList();
        }
    }

    private static class MemMarketDataTradeTapeStore implements MarketDataTradeTapeStore {
        private final List<TradeTapeItem> items = new java.util.ArrayList<>();

        @Override
        public void append(TradeTapeItem item) {
            items.add(item);
        }

        @Override
        public List<TradeTapeItem> findRecent(String symbol, int limit) {
            return items.stream()
                    .filter(item -> item.symbol().equals(symbol))
                    .sorted(java.util.Comparator.comparing(TradeTapeItem::ts).reversed())
                    .limit(Math.max(1, limit))
                    .toList();
        }
    }

    private static class MemMarketDataTickerStore implements MarketDataTickerStore {
        private final Map<String, MarketTicker> tickers = new LinkedHashMap<>();

        @Override
        public void save(MarketTicker ticker) {
            tickers.put(ticker.symbol(), ticker);
        }

        @Override
        public Optional<MarketTicker> find(String symbol) {
            return Optional.ofNullable(tickers.get(symbol));
        }
    }

    private static class MemMarketDataKlineStore implements MarketDataKlineStore {
        private final Map<String, MarketKline> klines = new LinkedHashMap<>();

        @Override
        public MarketKline save(MarketKline kline) {
            klines.put(key(kline.symbol(), kline.interval(), kline.openTime()), kline);
            return kline;
        }

        @Override
        public Optional<MarketKline> find(String symbol, String interval, Instant openTime) {
            return Optional.ofNullable(klines.get(key(symbol, interval, openTime)));
        }

        @Override
        public List<MarketKline> findRecent(String symbol, String interval, int limit) {
            return klines.values().stream()
                    .filter(kline -> kline.symbol().equals(symbol))
                    .filter(kline -> kline.interval().equals(interval))
                    .sorted(java.util.Comparator.comparing(MarketKline::openTime).reversed())
                    .limit(Math.max(1, limit))
                    .toList();
        }

        private static String key(String symbol, String interval, Instant openTime) {
            return symbol + ":" + interval + ":" + openTime;
        }
    }
}
