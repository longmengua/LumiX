package com.lumix.marketdata.aggregation;

import com.lumix.marketdata.contract.AtomicQuantity;
import com.lumix.marketdata.contract.DecimalPrice;
import com.lumix.marketdata.contract.Sequence;
import java.time.Instant;
import java.util.Objects;

/**
 * 單一來源時間 bucket 的 immutable OHLCV snapshot。
 *
 * <p>volume 僅由已接受的 normalized public trade 加總，不代表 LumiX execution volume，也不會回寫任何交易狀態。</p>
 */
public record CandleView(
        CandleInterval interval,
        CandleWindow window,
        DecimalPrice open,
        DecimalPrice high,
        DecimalPrice low,
        DecimalPrice close,
        AtomicQuantity baseVolume,
        QuoteVolume quoteVolume,
        int tradeCount,
        Sequence asOfSequence,
        Instant asOfSourceTimestamp
) {

    public CandleView {
        interval = Objects.requireNonNull(interval, "interval must not be null");
        window = Objects.requireNonNull(window, "window must not be null");
        open = Objects.requireNonNull(open, "open must not be null");
        high = Objects.requireNonNull(high, "high must not be null");
        low = Objects.requireNonNull(low, "low must not be null");
        close = Objects.requireNonNull(close, "close must not be null");
        baseVolume = Objects.requireNonNull(baseVolume, "baseVolume must not be null");
        quoteVolume = Objects.requireNonNull(quoteVolume, "quoteVolume must not be null");
        asOfSequence = Objects.requireNonNull(asOfSequence, "asOfSequence must not be null");
        asOfSourceTimestamp = Objects.requireNonNull(asOfSourceTimestamp, "asOfSourceTimestamp must not be null");
        if (tradeCount < 1 || !baseVolume.isPositive() || quoteVolume.atoms().signum() <= 0) {
            throw new IllegalArgumentException("candle requires at least one positive trade");
        }
    }
}
