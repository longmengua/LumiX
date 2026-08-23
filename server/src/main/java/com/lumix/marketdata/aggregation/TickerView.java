package com.lumix.marketdata.aggregation;

import com.lumix.marketdata.contract.AtomicQuantity;
import com.lumix.marketdata.contract.DecimalPrice;
import com.lumix.marketdata.contract.Sequence;
import java.time.Instant;
import java.util.Objects;

/**
 * 由來源事件時間 24 小時 rolling window 導出的唯讀 ticker snapshot。
 *
 * <p>沒有 trade 時 reducer 會保留空 ticker，而非填入零價格或零成交量。這避免 consumer 把不存在的市場資料
 * 誤解成真實價格。</p>
 */
public record TickerView(
        Instant windowStartInclusive,
        Instant windowEndInclusive,
        DecimalPrice open,
        DecimalPrice high,
        DecimalPrice low,
        DecimalPrice last,
        AtomicQuantity baseVolume,
        QuoteVolume quoteVolume,
        int tradeCount,
        Sequence asOfSequence,
        Instant asOfSourceTimestamp
) {

    public TickerView {
        windowStartInclusive = Objects.requireNonNull(windowStartInclusive, "windowStartInclusive must not be null");
        windowEndInclusive = Objects.requireNonNull(windowEndInclusive, "windowEndInclusive must not be null");
        open = Objects.requireNonNull(open, "open must not be null");
        high = Objects.requireNonNull(high, "high must not be null");
        low = Objects.requireNonNull(low, "low must not be null");
        last = Objects.requireNonNull(last, "last must not be null");
        baseVolume = Objects.requireNonNull(baseVolume, "baseVolume must not be null");
        quoteVolume = Objects.requireNonNull(quoteVolume, "quoteVolume must not be null");
        asOfSequence = Objects.requireNonNull(asOfSequence, "asOfSequence must not be null");
        asOfSourceTimestamp = Objects.requireNonNull(asOfSourceTimestamp, "asOfSourceTimestamp must not be null");
        if (windowStartInclusive.isAfter(windowEndInclusive)
                || tradeCount < 1
                || !baseVolume.isPositive()
                || quoteVolume.atoms().signum() <= 0) {
            throw new IllegalArgumentException("ticker requires a non-empty positive source-time window");
        }
    }
}
