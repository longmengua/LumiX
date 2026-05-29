/*
 * 檔案用途：Repository 介面，定義 market ticker 持久化與 latest-state 查詢契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.MarketTicker;

import java.util.Optional;

public interface MarketDataTickerStore {

    void save(MarketTicker ticker);

    Optional<MarketTicker> find(String symbol);
}
