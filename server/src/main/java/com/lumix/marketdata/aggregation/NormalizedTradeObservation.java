package com.lumix.marketdata.aggregation;

import com.lumix.marketdata.contract.AtomicQuantity;
import com.lumix.marketdata.contract.DecimalPrice;
import com.lumix.marketdata.contract.MarketDataEventIdentity;
import com.lumix.marketdata.contract.Sequence;
import java.time.Instant;
import java.util.Objects;

/**
 * 已被 reducer 接納的一筆 normalized public trade 唯讀觀測值。
 *
 * <p>這不是 LumiX fill，也不包含 order、帳本、position 或 settlement 欄位；保留 identity 與來源時間
 * 是為了讓 ticker window、duplicate 防線及 audit/replay 可重現。</p>
 */
public record NormalizedTradeObservation(
        MarketDataEventIdentity identity,
        Sequence sequence,
        Instant sourceTimestamp,
        DecimalPrice price,
        AtomicQuantity quantity,
        QuoteVolume quoteVolume
) {

    public NormalizedTradeObservation {
        identity = Objects.requireNonNull(identity, "identity must not be null");
        sequence = Objects.requireNonNull(sequence, "sequence must not be null");
        sourceTimestamp = Objects.requireNonNull(sourceTimestamp, "sourceTimestamp must not be null");
        price = Objects.requireNonNull(price, "price must not be null");
        quantity = Objects.requireNonNull(quantity, "quantity must not be null");
        quoteVolume = Objects.requireNonNull(quoteVolume, "quoteVolume must not be null");
        if (!price.isPositive() || !quantity.isPositive()) {
            throw new IllegalArgumentException("aggregated trade price and quantity must be positive");
        }
    }
}
