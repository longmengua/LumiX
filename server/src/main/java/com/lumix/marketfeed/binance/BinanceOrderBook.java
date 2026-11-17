package com.lumix.marketfeed.binance;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * 單一交易對的 Binance 唯讀行情簿；不接受或產生交易訂單。
 * 所有寫入以同一鎖序列化，避免快照與增量交錯造成錯誤的健康狀態。
 */
public final class BinanceOrderBook {
    private static final int MAX_LEVELS = 10000;
    private NavigableMap<BigDecimal, BigDecimal> bids = new TreeMap<>(Comparator.reverseOrder());
    private NavigableMap<BigDecimal, BigDecimal> asks = new TreeMap<>();
    private long sequence;
    private Instant updatedAt;
    private boolean synchronizedBook;

    /** 來源價位，全部以十進位字串解析，不經 binary floating point。 */
    public record Level(String price, String quantity) { }
    /** 不可變快照；不健康時不對外暴露舊委託簿。 */
    public record View(boolean healthy, long sequence, Instant receivedAt, List<Level> bids, List<Level> asks) { }

    /** 斷線或資料無法驗證時立即停用舊快照。 */
    public synchronized void invalidate() {
        synchronizedBook = false;
        updatedAt = null;
    }

    /** 原子建立來源快照；必須經連續增量銜接後才能視為健康。 */
    public synchronized void snapshot(long updateId, List<Level> bidLevels, List<Level> askLevels, Instant receivedAt) {
        synchronizedBook = false;
        updatedAt = null;
        if (updateId < 0 || receivedAt == null) throw new IllegalArgumentException("invalid snapshot metadata");
        NavigableMap<BigDecimal, BigDecimal> nextBids = new TreeMap<>(Comparator.reverseOrder());
        NavigableMap<BigDecimal, BigDecimal> nextAsks = new TreeMap<>();
        apply(nextBids, bidLevels);
        apply(nextAsks, askLevels);
        validate(nextBids, nextAsks);
        bids = nextBids;
        asks = nextAsks;
        sequence = updateId;
        updatedAt = receivedAt;
    }

    /** 只有區間連續的增量可原子套用；缺口或錯誤使整份行情失效。 */
    public synchronized BinanceDepthSequence.Decision delta(long first, long last, List<Level> bidLevels, List<Level> askLevels, Instant receivedAt) {
        try {
            if (updatedAt == null || receivedAt == null || receivedAt.isBefore(updatedAt)) throw new IllegalStateException("snapshot and monotonic receive time required");
            var decision = BinanceDepthSequence.evaluate(sequence, first, last);
            if (decision == BinanceDepthSequence.Decision.OBSOLETE) return decision;
            if (decision == BinanceDepthSequence.Decision.RESYNC_REQUIRED) {
                synchronizedBook = false;
                updatedAt = null;
                return decision;
            }
            NavigableMap<BigDecimal, BigDecimal> nextBids = new TreeMap<>(bids);
            NavigableMap<BigDecimal, BigDecimal> nextAsks = new TreeMap<>(asks);
            apply(nextBids, bidLevels);
            apply(nextAsks, askLevels);
            validate(nextBids, nextAsks);
            bids = nextBids;
            asks = nextAsks;
            sequence = last;
            updatedAt = receivedAt;
            synchronizedBook = true;
            return decision;
        } catch (RuntimeException exception) {
            synchronizedBook = false;
            updatedAt = null;
            throw exception;
        }
    }

    /** 過期時隱藏全部價位，避免下游把舊資料當作即時行情。 */
    public synchronized View view(Instant now, Duration maximumAge) {
        if (now == null || maximumAge == null || maximumAge.isZero() || maximumAge.isNegative()) throw new IllegalArgumentException("positive freshness window required");
        boolean healthy = synchronizedBook && updatedAt != null && !now.isBefore(updatedAt) && Duration.between(updatedAt, now).compareTo(maximumAge) < 0;
        return new View(healthy, sequence, updatedAt, healthy ? levels(bids) : List.of(), healthy ? levels(asks) : List.of());
    }

    private static void apply(NavigableMap<BigDecimal, BigDecimal> target, List<Level> levels) {
        if (levels == null || levels.size() > MAX_LEVELS) throw new IllegalArgumentException("invalid level count");
        for (Level level : levels) {
            if (level == null) throw new IllegalArgumentException("null level");
            BigDecimal price = decimal(level.price());
            BigDecimal quantity = decimal(level.quantity());
            if (price.signum() <= 0 || quantity.signum() < 0) throw new IllegalArgumentException("invalid price or quantity");
            if (quantity.signum() == 0) target.remove(price); else target.put(price, quantity);
        }
    }

    private static BigDecimal decimal(String value) {
        if (value == null || value.length() > 64 || !value.matches("[0-9]+(\\.[0-9]+)?")) throw new IllegalArgumentException("invalid decimal");
        BigDecimal parsed = new BigDecimal(value);
        if (parsed.precision() > 38 || parsed.scale() > 18) throw new IllegalArgumentException("decimal exceeds precision");
        return parsed;
    }

    private static void validate(NavigableMap<BigDecimal, BigDecimal> bids, NavigableMap<BigDecimal, BigDecimal> asks) {
        if (bids.size() > MAX_LEVELS || asks.size() > MAX_LEVELS || bids.isEmpty() || asks.isEmpty() || bids.firstKey().compareTo(asks.firstKey()) >= 0) throw new IllegalArgumentException("invalid or crossed book");
    }

    private static List<Level> levels(NavigableMap<BigDecimal, BigDecimal> values) {
        return values.entrySet().stream().map(entry -> new Level(entry.getKey().toPlainString(), entry.getValue().toPlainString())).toList();
    }
}
