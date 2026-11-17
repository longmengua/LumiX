package com.lumix.marketfeed.binance;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BinanceKlineSeriesTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:01:30Z");
    private static final long T = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
    private static BinanceKlineSeries.Candle candle(long start, String high, String close, String volume, long count, boolean closed) {
        return new BinanceKlineSeries.Candle(start, "100.00000001", high, "99", close, volume, count, closed);
    }

    @Test void snapshotRequiresLiveEventAndPreservesExactValues() {
        // 只有真實來源更新接續歷史後，才可向圖表標示為健康。
        var series = new BinanceKlineSeries();
        series.snapshot(List.of(candle(T - 60_000, "101", "100", "1", 1, true), candle(T, "101", "100", "1", 1, false)));
        assertFalse(series.view(NOW).healthy());
        series.update(candle(T, "102", "101", "2", 2, false), NOW, NOW);
        assertTrue(series.view(NOW).healthy());
        assertEquals("100.00000001", series.view(NOW).candles().getLast().open());
        assertFalse(series.view(NOW.plusSeconds(8)).healthy());
    }

    @Test void rejectsGapAndRegressedCandle() {
        // 漏收收線或 OHLC 倒退時必須重取歷史，不能延用舊圖。
        var series = new BinanceKlineSeries();
        series.snapshot(List.of(candle(T - 60_000, "101", "100", "1", 1, true), candle(T, "101", "100", "1", 1, false)));
        assertThrows(IllegalArgumentException.class, () -> series.update(candle(T + 60_000, "101", "100", "1", 1, false), NOW, NOW));
        assertFalse(series.view(NOW).healthy());
        series.snapshot(List.of(candle(T - 60_000, "101", "100", "1", 1, true), candle(T, "102", "100", "1", 1, false)));
        assertThrows(IllegalArgumentException.class, () -> series.update(candle(T, "101", "100", "2", 2, false), NOW, NOW));
        assertFalse(series.view(NOW).healthy());
    }

    @Test void olderBufferedUpdateDoesNotOverrideSnapshot() {
        // 快照可能比緩衝訊息更新；較少成交筆數的事件不可覆寫它。
        var series = new BinanceKlineSeries();
        series.snapshot(List.of(candle(T - 60_000, "101", "100", "1", 1, true), candle(T, "102", "101", "2", 2, false)));
        series.update(candle(T, "101", "100", "1", 1, false), NOW, NOW);
        assertFalse(series.view(NOW).healthy());
        series.update(candle(T, "102", "101", "2", 2, false), NOW, NOW);
        assertEquals("2", series.view(NOW).candles().getLast().volume());
    }
}
