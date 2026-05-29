/*
 * 檔案用途：測試 market-data history retention policy。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.DepthDelta;
import com.example.exchange.domain.model.dto.MarketKline;
import com.example.exchange.domain.model.dto.TradeTapeItem;
import com.example.exchange.domain.repository.MarketDataDepthDeltaStore;
import com.example.exchange.domain.repository.MarketDataKlineStore;
import com.example.exchange.domain.repository.MarketDataTradeTapeStore;
import com.example.exchange.infra.config.MarketDataRetentionProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MarketDataRetentionServiceTest {

    @Test
    @DisplayName("market data retention 依各 history window 清理過期資料")
    /**
     * 流程：depth delta、trade tape、kline 使用不同 retention window。
     * 期望：service 對每個 store 傳入各自 cutoff，且回傳已清理筆數；ticker latest-state 與 sequence checkpoint 不在清理範圍。
     */
    void purgesMarketDataHistoryWithIndependentRetentionWindows() {
        CountingDepthDeltaStore depthDeltaStore = new CountingDepthDeltaStore(2);
        CountingTradeTapeStore tradeTapeStore = new CountingTradeTapeStore(3);
        CountingKlineStore klineStore = new CountingKlineStore(4);
        MarketDataRetentionProperties properties = new MarketDataRetentionProperties();
        properties.setDepthDelta(Duration.ofHours(6));
        properties.setTradeTape(Duration.ofDays(2));
        properties.setKline(Duration.ofDays(14));
        MarketDataRetentionService service = new MarketDataRetentionService(
                depthDeltaStore,
                tradeTapeStore,
                klineStore,
                properties
        );

        Instant now = Instant.parse("2026-05-29T07:00:00Z");
        MarketDataRetentionService.RetentionResult result = service.purgeExpired(now);

        assertThat(depthDeltaStore.cutoff).isEqualTo(now.minus(Duration.ofHours(6)));
        assertThat(tradeTapeStore.cutoff).isEqualTo(now.minus(Duration.ofDays(2)));
        assertThat(klineStore.cutoff).isEqualTo(now.minus(Duration.ofDays(14)));
        assertThat(result.depthDeltas()).isEqualTo(2);
        assertThat(result.tradeTape()).isEqualTo(3);
        assertThat(result.klines()).isEqualTo(4);
    }

    @Test
    @DisplayName("market data retention window 為 zero 時不清理該類資料")
    /**
     * 流程：depth delta retention 設為 zero，trade/kline 保持正常。
     * 期望：depth store 不收到 purge cutoff，避免錯把 zero 視為立即刪除所有資料。
     */
    void zeroRetentionWindowSkipsThatHistoryType() {
        CountingDepthDeltaStore depthDeltaStore = new CountingDepthDeltaStore(2);
        CountingTradeTapeStore tradeTapeStore = new CountingTradeTapeStore(3);
        CountingKlineStore klineStore = new CountingKlineStore(4);
        MarketDataRetentionProperties properties = new MarketDataRetentionProperties();
        properties.setDepthDelta(Duration.ZERO);
        properties.setTradeTape(Duration.ofDays(2));
        properties.setKline(Duration.ofDays(14));
        MarketDataRetentionService service = new MarketDataRetentionService(
                depthDeltaStore,
                tradeTapeStore,
                klineStore,
                properties
        );

        MarketDataRetentionService.RetentionResult result = service.purgeExpired(Instant.parse("2026-05-29T07:00:00Z"));

        assertThat(depthDeltaStore.cutoff).isNull();
        assertThat(result.depthDeltas()).isZero();
        assertThat(result.tradeTape()).isEqualTo(3);
        assertThat(result.klines()).isEqualTo(4);
    }

    private static class CountingDepthDeltaStore implements MarketDataDepthDeltaStore {
        private final long deleted;
        private Instant cutoff;

        private CountingDepthDeltaStore(long deleted) {
            this.deleted = deleted;
        }

        @Override
        public void append(DepthDelta delta) {
        }

        @Override
        public List<DepthDelta> findAfter(String symbol, long afterVersion, int limit) {
            return List.of();
        }

        @Override
        public long purgeBefore(Instant cutoff) {
            this.cutoff = cutoff;
            return deleted;
        }
    }

    private static class CountingTradeTapeStore implements MarketDataTradeTapeStore {
        private final long deleted;
        private Instant cutoff;

        private CountingTradeTapeStore(long deleted) {
            this.deleted = deleted;
        }

        @Override
        public void append(TradeTapeItem item) {
        }

        @Override
        public List<TradeTapeItem> findRecent(String symbol, int limit) {
            return List.of();
        }

        @Override
        public long purgeBefore(Instant cutoff) {
            this.cutoff = cutoff;
            return deleted;
        }
    }

    private static class CountingKlineStore implements MarketDataKlineStore {
        private final long deleted;
        private Instant cutoff;

        private CountingKlineStore(long deleted) {
            this.deleted = deleted;
        }

        @Override
        public MarketKline save(MarketKline kline) {
            return kline;
        }

        @Override
        public Optional<MarketKline> find(String symbol, String interval, Instant openTime) {
            return Optional.empty();
        }

        @Override
        public List<MarketKline> findRecent(String symbol, String interval, int limit) {
            return List.of();
        }

        @Override
        public long purgeBefore(Instant cutoff) {
            this.cutoff = cutoff;
            return deleted;
        }
    }
}
