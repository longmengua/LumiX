/*
 * 檔案用途：Repository 介面，定義 market-data durable sequence checkpoint 存取契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.MarketDataSequenceCheckpoint;

import java.util.Optional;

public interface MarketDataSequenceCheckpointStore {

    Optional<MarketDataSequenceCheckpoint> find(String symbol, String stream);

    MarketDataSequenceCheckpoint save(MarketDataSequenceCheckpoint checkpoint);
}
