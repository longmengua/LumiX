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
        // TODO(HUMAN_REVIEW_REQUIRED): 目前回傳空清單，避免把未審核的 symbol metadata 誤當成正式行情來源。
        return List.of();
    }

    @Override
    public OrderBookSnapshot getDepth(String symbol, int limit) {
        validateSymbol(symbol);
        validateLimit(limit);

        // TODO(HUMAN_REVIEW_REQUIRED): 目前不回傳真實 order book，以免把未接好的 market feed 或 matching output 當成正式資料。
        return new OrderBookSnapshot(symbol.trim(), SymbolType.SPOT, Instant.now(), List.of(), List.of());
    }

    @Override
    public List<MarketTradeView> getRecentTrades(String symbol, int limit) {
        validateSymbol(symbol);
        validateLimit(limit);

        // TODO(HUMAN_REVIEW_REQUIRED): 目前不串接真實成交事件流，避免把 placeholder 當成可用的市場資料管線。
        return List.of();
    }

    @Override
    public Optional<Ticker24hView> getTicker24h(String symbol) {
        validateSymbol(symbol);

        // TODO(HUMAN_REVIEW_REQUIRED): 目前不計算真實 24h ticker，避免未定義的統計來源影響前端顯示。
        return Optional.empty();
    }

    @Override
    public List<KlineView> getKline(String symbol, KlineInterval interval, int limit) {
        validateSymbol(symbol);
        Objects.requireNonNull(interval, "interval must not be null");
        validateLimit(limit);

        // TODO(HUMAN_REVIEW_REQUIRED): 目前不聚合或發布正式 K 線資料，避免把 stub 當成正式行情服務。
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
