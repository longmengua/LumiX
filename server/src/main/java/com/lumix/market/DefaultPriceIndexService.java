package com.lumix.market;

import com.lumix.common.MoneyAmount;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Phase 10 指數價格 stub。
 * 只提供 placeholder response，不做真實來源聚合或公式計算。
 */
public class DefaultPriceIndexService implements PriceIndexService {

    @Override
    public List<ExternalPriceQuote> listExternalQuotes(String symbol) {
        validateSymbol(symbol);

        // TODO: requires high-reasoning review before production use
        // Placeholder only. This stub does not fetch or normalize any external venue quote.
        return List.of();
    }

    @Override
    public PriceIndexView getPriceIndex(String symbol) {
        validateSymbol(symbol);

        // TODO: requires high-reasoning review before production use
        // Placeholder only. This stub does not implement weighted average, median, or outlier rejection.
        return new PriceIndexView(symbol.trim(), MoneyAmount.zero(), 0, Instant.now());
    }

    private void validateSymbol(String symbol) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        if (symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
    }
}
