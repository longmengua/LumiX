package com.lumix.marketfeed.binance;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** 具名週期來源 K 線的有限唯讀序列；不參與下單、風控或資產計算。 */
public final class BinanceKlineSeries {
    private static final int LIMIT = 320;
    private final BinanceKlineInterval interval;

    /** 每條序列必須綁定唯一來源週期，避免跨週期誤套用快照或事件。 */
    public BinanceKlineSeries(BinanceKlineInterval interval) { this.interval = java.util.Objects.requireNonNull(interval); }
    private List<Candle> candles = List.of();
    private Instant sourceAt;
    private Instant receivedAt;
    private boolean live;

    /** 價格與數量保持來源十進位字串；tradeCount 用於阻止較舊更新覆寫較新資料。 */
    public record Candle(long openTime, String open, String high, String low, String close,
                         String volume, long tradeCount, boolean closed) { }
    /** 不健康時不提供舊 K 線，避免下游誤認為即時。 */
    public record View(boolean healthy, String interval, Instant sourceAt, Instant receivedAt, List<Candle> candles) { }

    /** 與來源中斷後必須重新載入歷史，不能繼續沿用舊序列。 */
    public synchronized void invalidate() {
        live = false;
        candles = List.of();
        sourceAt = null;
        receivedAt = null;
    }

    /** 先建立嚴格連續的歷史，再等待有效即時事件才能標示健康。 */
    public synchronized void snapshot(List<Candle> history) {
        invalidate();
        if (history == null || history.size() < 2 || history.size() > LIMIT) throw new IllegalArgumentException("invalid candle history size");
        List<Candle> result = new ArrayList<>(history.size());
        long previous = -1;
        for (int i = 0; i < history.size(); i++) {
            Candle value = history.get(i);
            validate(value);
            if (previous >= 0 && value.openTime() != interval.nextOpen(previous)) throw new IllegalArgumentException("candle history gap");
            if (i < history.size() - 1 && !value.closed()) throw new IllegalArgumentException("historical candle must be closed");
            result.add(value);
            previous = value.openTime();
        }
        candles = List.copyOf(result);
    }

    /** 只接受同一週期的非倒退更新或已收線後的連續下一週期。 */
    public synchronized void update(Candle candle, Instant eventTime, Instant arrivalTime) {
        try {
            validate(candle);
            if (candles.isEmpty() || eventTime == null || arrivalTime == null || eventTime.isBefore(arrivalTime.minusSeconds(10))
                    || eventTime.isAfter(arrivalTime.plusSeconds(5)) || (sourceAt != null && eventTime.isBefore(sourceAt))
                    || eventTime.toEpochMilli() < candle.openTime() || eventTime.toEpochMilli() > Math.addExact(interval.nextOpen(candle.openTime()), 30_000)) {
                throw new IllegalArgumentException("invalid candle event time or missing snapshot");
            }
            Candle previous = candles.getLast();
            if (candle.openTime() < previous.openTime()) return;
            List<Candle> result = new ArrayList<>(candles);
            if (candle.openTime() == previous.openTime()) {
                if (candle.tradeCount() < previous.tradeCount()) return;
                if ((previous.closed() && !candle.closed()) || !samePrice(previous.open(), candle.open())
                        || (previous.closed() && (candle.tradeCount() != previous.tradeCount()
                            || !samePrice(previous.high(), candle.high()) || !samePrice(previous.low(), candle.low())
                            || !samePrice(previous.close(), candle.close()) || !samePrice(previous.volume(), candle.volume())))
                        || decimal(candle.high()).compareTo(decimal(previous.high())) < 0
                        || decimal(candle.low()).compareTo(decimal(previous.low())) > 0
                        || decimal(candle.volume()).compareTo(decimal(previous.volume())) < 0) {
                    throw new IllegalArgumentException("candle regressed");
                }
                result.set(result.size() - 1, candle);
            } else {
                if (candle.openTime() != interval.nextOpen(previous.openTime()) || !previous.closed()) throw new IllegalArgumentException("candle continuity gap");
                result.add(candle);
                if (result.size() > LIMIT) result.removeFirst();
            }
            candles = List.copyOf(result);
            sourceAt = eventTime;
            receivedAt = arrivalTime;
            live = true;
        } catch (RuntimeException exception) {
            invalidate();
            throw exception;
        }
    }

    /** 來源或接收時間過期時清空輸出，不能把最後一張圖當即時資料。 */
    public synchronized View view(Instant now) {
        if (now == null) throw new IllegalArgumentException("timestamp required");
        boolean healthy = live && sourceAt != null && receivedAt != null && !now.isBefore(receivedAt)
                && Duration.between(receivedAt, now).compareTo(Duration.ofSeconds(8)) < 0
                && !sourceAt.isBefore(now.minusSeconds(10));
        return new View(healthy, interval.value(), sourceAt, receivedAt, healthy ? candles : List.of());
    }

    private void validate(Candle value) {
        if (value == null || !interval.aligned(value.openTime()) || value.tradeCount() < 0) throw new IllegalArgumentException("invalid candle metadata");
        BigDecimal open = decimal(value.open()), high = decimal(value.high()), low = decimal(value.low());
        BigDecimal close = decimal(value.close()), volume = decimal(value.volume());
        if (open.signum() <= 0 || low.signum() <= 0 || close.signum() <= 0 || volume.signum() < 0
                || high.compareTo(open) < 0 || high.compareTo(close) < 0 || low.compareTo(open) > 0
                || low.compareTo(close) > 0 || high.compareTo(low) < 0) throw new IllegalArgumentException("invalid candle prices");
    }

    private static BigDecimal decimal(String value) {
        if (value == null || value.length() > 64 || !value.matches("[0-9]+(\\.[0-9]+)?")) throw new IllegalArgumentException("invalid decimal");
        BigDecimal decimal = new BigDecimal(value);
        if (decimal.precision() > 38 || decimal.scale() > 18) throw new IllegalArgumentException("decimal exceeds precision");
        return decimal;
    }
    private static boolean samePrice(String first, String second) { return decimal(first).compareTo(decimal(second)) == 0; }
}
