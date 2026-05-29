/*
 * 檔案用途：Repository 介面，定義 market-data kline 持久化與查詢契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.MarketKline;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MarketDataKlineStore {

    MarketKline save(MarketKline kline);

    Optional<MarketKline> find(String symbol, String interval, Instant openTime);

    List<MarketKline> findRecent(String symbol, String interval, int limit);

    default long purgeBefore(Instant cutoff) {
        return 0L;
    }
}
