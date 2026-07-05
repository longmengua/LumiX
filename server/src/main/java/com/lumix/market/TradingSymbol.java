package com.lumix.market;

import com.lumix.account.AssetSymbol;

import java.util.Objects;

/**
 * 交易對展示模型。
 */
public record TradingSymbol(
        String symbol,
        AssetSymbol baseAsset,
        AssetSymbol quoteAsset,
        SymbolType type,
        boolean tradingEnabled
) {

    public TradingSymbol {
        symbol = requireText(symbol, "symbol");
        Objects.requireNonNull(baseAsset, "baseAsset must not be null");
        Objects.requireNonNull(quoteAsset, "quoteAsset must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
