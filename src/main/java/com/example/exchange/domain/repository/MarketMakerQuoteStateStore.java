/*
 * 檔案用途：做市商 quote active-state store，保存重啟後可恢復查詢的 quote ownership。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.MarketMakerQuoteState;

import java.util.List;
import java.util.Optional;

public interface MarketMakerQuoteStateStore {

    void save(MarketMakerQuoteState state);

    Optional<MarketMakerQuoteState> find(String marketMakerId, String symbol);

    List<MarketMakerQuoteState> findByMarketMakerId(String marketMakerId, int limit);

    List<MarketMakerQuoteState> findActive(int limit);
}
