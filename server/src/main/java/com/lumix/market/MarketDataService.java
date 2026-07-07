package com.lumix.market;

import java.util.List;
import java.util.Optional;

/**
 * 行情服務契約。
 */
public interface MarketDataService {

    // TODO(HUMAN_REVIEW_REQUIRED): 列出可交易 symbol，供行情頁與下單表單使用。
    List<TradingSymbol> listSymbols();

    // TODO(HUMAN_REVIEW_REQUIRED): 取得 order book 深度。
    OrderBookSnapshot getDepth(String symbol, int limit);

    // TODO(HUMAN_REVIEW_REQUIRED): 取得最近成交列表。
    List<MarketTradeView> getRecentTrades(String symbol, int limit);

    // TODO(HUMAN_REVIEW_REQUIRED): 取得 24h ticker。
    Optional<Ticker24hView> getTicker24h(String symbol);

    // TODO(HUMAN_REVIEW_REQUIRED): 取得 K 線資料。
    List<KlineView> getKline(String symbol, KlineInterval interval, int limit);
}
