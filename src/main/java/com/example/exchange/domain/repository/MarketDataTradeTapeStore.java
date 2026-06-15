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

    default List<TradeTapeItem> findAfter(String symbol, Instant afterTs, String afterMatchId, int limit) {
        return List.of();
    }

    default List<TradeTapeItem> findBefore(String symbol, Instant beforeTs, String beforeMatchId, int limit) {
        return List.of();
    }

    default List<TradeTapeItem> findByMatchId(String matchId) {
        return List.of();
    }

    default long purgeBefore(Instant cutoff) {
        return 0L;
    }
}
