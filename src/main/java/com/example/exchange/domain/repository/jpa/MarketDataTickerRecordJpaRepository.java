/*
 * 檔案用途：Spring Data JPA repository，提供 market ticker latest-state 查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MarketDataTickerRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketDataTickerRecordJpaRepository extends JpaRepository<MarketDataTickerRecord, String> {
}
