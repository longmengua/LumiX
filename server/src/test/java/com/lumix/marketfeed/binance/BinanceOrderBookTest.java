package com.lumix.marketfeed.binance;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BinanceOrderBookTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final List<BinanceOrderBook.Level> BIDS = List.of(new BinanceOrderBook.Level("100.00000001", "0.12345678"));
    private static final List<BinanceOrderBook.Level> ASKS = List.of(new BinanceOrderBook.Level("101.00", "2"));

    @Test
    void requiresContinuousDeltaAfterSnapshotAndPreservesDecimalStrings() {
        // 快照在銜接來源增量之前不能視為即時；數值不可經浮點數改寫。
        var book = new BinanceOrderBook();
        book.snapshot(100, BIDS, ASKS, NOW);
        assertFalse(book.view(NOW, Duration.ofSeconds(5)).healthy());
        book.delta(99, 102, List.of(), List.of(), NOW);
        var view = book.view(NOW, Duration.ofSeconds(5));
        assertTrue(view.healthy());
        assertEquals("100.00000001", view.bids().getFirst().price());
        assertEquals("0.12345678", view.bids().getFirst().quantity());
        assertFalse(book.view(NOW.plusSeconds(5), Duration.ofSeconds(5)).healthy());
    }

    @Test
    void gapAndInvalidUpdatesRequireNewSnapshot() {
        // 缺號或不合法行情不能以後續增量自動恢復，必須重新建立快照。
        var book = new BinanceOrderBook();
        book.snapshot(100, BIDS, ASKS, NOW);
        book.delta(101, 101, List.of(), List.of(), NOW);
        assertEquals(BinanceDepthSequence.Decision.RESYNC_REQUIRED, book.delta(103, 104, List.of(), List.of(), NOW));
        assertFalse(book.view(NOW, Duration.ofSeconds(5)).healthy());
        assertThrows(IllegalStateException.class, () -> book.delta(102, 104, List.of(), List.of(), NOW));
        book.snapshot(104, BIDS, ASKS, NOW);
        assertThrows(IllegalArgumentException.class, () -> book.delta(105, 105, List.of(new BinanceOrderBook.Level("102", "1")), List.of(), NOW));
        assertFalse(book.view(NOW, Duration.ofSeconds(5)).healthy());
    }
}
