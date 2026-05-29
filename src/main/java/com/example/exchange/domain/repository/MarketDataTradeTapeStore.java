/*
 * 檔案用途：Repository 介面，定義 trade tape 持久化與查詢契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.TradeTapeItem;

import java.time.Instant;
import java.util.List;

public interface MarketDataTradeTapeStore {

    void append(TradeTapeItem item);

    List<TradeTapeItem> findRecent(String symbol, int limit);

    default long purgeBefore(Instant cutoff) {
        return 0L;
    }
}
