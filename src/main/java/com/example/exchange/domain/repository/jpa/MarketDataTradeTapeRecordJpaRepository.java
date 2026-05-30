/*
 * 檔案用途：Spring Data JPA repository，提供 market-data trade tape 查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MarketDataTradeTapeRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MarketDataTradeTapeRecordJpaRepository
        extends JpaRepository<MarketDataTradeTapeRecord, Long> {

    List<MarketDataTradeTapeRecord> findBySymbolOrderByTradeTsDescIdDesc(String symbol, Pageable pageable);

    List<MarketDataTradeTapeRecord> findByMatchIdOrderByTradeTsAscIdAsc(String matchId);

    long deleteByTradeTsBefore(Instant cutoff);
}
