/*
 * 檔案用途：Spring Data JPA repository，提供 market-data sequence checkpoint 查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MarketDataSequenceCheckpointRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketDataSequenceCheckpointRecordJpaRepository
        extends JpaRepository<MarketDataSequenceCheckpointRecord, MarketDataSequenceCheckpointRecord.Key> {
}
