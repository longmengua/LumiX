package com.lumix.market;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Phase 10 行情 stub。
 * 不接任何資料庫、快取、消息總線、WebSocket 或外部行情源。
 */
public class DefaultMarketDataService implements MarketDataService {

    @Override
    public List<TradingSymbol> listSymbols() {
        // TODO: requires high-reasoning review before production use
        // Placeholder only. Real symbol metadata should come from reviewed configuration and exchange rules.
        return List.of();
    }

    @Override
    public OrderBookSnapshot getDepth(String symbol, int limit) {
        validateSymbol(symbol);
        validateLimit(limit);

        // TODO: requires high-reasoning review before production use
        // Placeholder only. This stub does not represent a real order book, market feed, or matching output.
        return new OrderBookSnapshot(symbol.trim(), SymbolType.SPOT, Instant.now(), List.of(), List.of());
    }

    @Override
    public List<MarketTradeView> getRecentTrades(String symbol, int limit) {
        validateSymbol(symbol);
        validateLimit(limit);

        // TODO: requires high-reasoning review before production use
        // Placeholder only. This stub does not stream or persist real trade events.
        return List.of();
    }

    @Override
    public Optional<Ticker24hView> getTicker24h(String symbol) {
        validateSymbol(symbol);

        // TODO: requires high-reasoning review before production use
        // Placeholder only. This stub intentionally does not claim a real 24h ticker calculation.
        return Optional.empty();
    }

    @Override
    public List<KlineView> getKline(String symbol, KlineInterval interval, int limit) {
        validateSymbol(symbol);
        Objects.requireNonNull(interval, "interval must not be null");
        validateLimit(limit);

        // TODO: requires high-reasoning review before production use
        // Placeholder only. This stub intentionally does not aggregate or publish production candle data.
        return List.of();
    }

    private void validateSymbol(String symbol) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        if (symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
    }

    private void validateLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }
    }
}
