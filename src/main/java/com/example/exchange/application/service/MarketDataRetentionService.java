/*
 * 檔案用途：應用服務，依設定清理高流量 market-data history。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.repository.MarketDataDepthDeltaStore;
import com.example.exchange.domain.repository.MarketDataKlineStore;
import com.example.exchange.domain.repository.MarketDataTradeTapeStore;
import com.example.exchange.infra.config.MarketDataRetentionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MarketDataRetentionService {

    private final MarketDataDepthDeltaStore depthDeltaStore;
    private final MarketDataTradeTapeStore tradeTapeStore;
    private final MarketDataKlineStore klineStore;
    private final MarketDataRetentionProperties properties;

    /**
     * 清理已超過 retention window 的高流量行情資料。
     * Ticker 是 latest-state table，不在此處刪除；sequence checkpoint 也保留作為 replay/reconnect 錨點。
     */
    public RetentionResult purgeExpired(Instant now) {
        Instant effectiveNow = now == null ? Instant.now() : now;
        long depthDeltas = purge(depthDeltaStore, effectiveNow, properties.getDepthDelta());
        long tradeTape = purge(tradeTapeStore, effectiveNow, properties.getTradeTape());
        long klines = purge(klineStore, effectiveNow, properties.getKline());
        return new RetentionResult(depthDeltas, tradeTape, klines);
    }

    private static long purge(MarketDataDepthDeltaStore store, Instant now, Duration retention) {
        return retention == null || retention.isNegative() || retention.isZero()
                ? 0L
                : store.purgeBefore(now.minus(retention));
    }

    private static long purge(MarketDataTradeTapeStore store, Instant now, Duration retention) {
        return retention == null || retention.isNegative() || retention.isZero()
                ? 0L
                : store.purgeBefore(now.minus(retention));
    }

    private static long purge(MarketDataKlineStore store, Instant now, Duration retention) {
        return retention == null || retention.isNegative() || retention.isZero()
                ? 0L
                : store.purgeBefore(now.minus(retention));
    }

    public record RetentionResult(long depthDeltas, long tradeTape, long klines) {
    }
}
