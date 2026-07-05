package com.lumix.market;

import java.util.List;
import java.util.Optional;

/**
 * 行情服務契約。
 */
public interface MarketDataService {

    // TODO: requires high-reasoning review before production use
    List<TradingSymbol> listSymbols();

    // TODO: requires high-reasoning review before production use
    OrderBookSnapshot getDepth(String symbol, int limit);

    // TODO: requires high-reasoning review before production use
    List<MarketTradeView> getRecentTrades(String symbol, int limit);

    // TODO: requires high-reasoning review before production use
    Optional<Ticker24hView> getTicker24h(String symbol);

    // TODO: requires high-reasoning review before production use
    List<KlineView> getKline(String symbol, KlineInterval interval, int limit);
}
