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

        // TODO(HUMAN_REVIEW_REQUIRED): 目前不抓取任何外部報價，避免把未接來源的資料當成正式 price index。
        return List.of();
    }

    @Override
    public PriceIndexView getPriceIndex(String symbol) {
        validateSymbol(symbol);

        // TODO(HUMAN_REVIEW_REQUIRED): 目前不實作 weighted average / median / outlier rejection，因為正式規則尚未在本階段定義。
        return new PriceIndexView(symbol.trim(), MoneyAmount.zero(), 0, Instant.now());
    }

    private void validateSymbol(String symbol) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        if (symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
    }
}
