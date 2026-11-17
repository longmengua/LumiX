package com.lumix.marketfeed.binance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BinanceDepthSequenceTest {
    @Test
    void acceptsOverlappingOrConsecutiveIntervalsWithoutInventingSequence() {
        // 來源聚合多次更新時，即使最後序號跳號，也必須辨識其區間仍覆蓋下一筆。
        assertEquals(BinanceDepthSequence.Decision.APPLY, BinanceDepthSequence.evaluate(100, 99, 104));
        assertEquals(BinanceDepthSequence.Decision.APPLY, BinanceDepthSequence.evaluate(104, 105, 110));
    }

    @Test
    void rejectsGapsAndIgnoresAlreadyAppliedIntervals() {
        // 遺失來源更新時不可繼續宣稱委託簿完整；舊資料則不得重複套用。
        assertEquals(BinanceDepthSequence.Decision.RESYNC_REQUIRED, BinanceDepthSequence.evaluate(100, 102, 105));
        assertEquals(BinanceDepthSequence.Decision.OBSOLETE, BinanceDepthSequence.evaluate(100, 90, 100));
        assertEquals(BinanceDepthSequence.Decision.OBSOLETE, BinanceDepthSequence.evaluate(Long.MAX_VALUE, 1, Long.MAX_VALUE));
    }

    @Test
    void rejectsMalformedRanges() {
        // 無效或倒退區間不可進入來源游標與投影。
        assertThrows(IllegalArgumentException.class, () -> BinanceDepthSequence.evaluate(100, 105, 104));
        assertThrows(IllegalArgumentException.class, () -> BinanceDepthSequence.evaluate(-1, 1, 2));
    }
}
