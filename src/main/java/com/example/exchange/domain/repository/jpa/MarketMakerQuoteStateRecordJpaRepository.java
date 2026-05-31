/*
 * 檔案用途：Spring Data JPA repository，提供做市商 quote active-state 查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MarketMakerQuoteStateRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketMakerQuoteStateRecordJpaRepository extends JpaRepository<MarketMakerQuoteStateRecord, String> {

    Optional<MarketMakerQuoteStateRecord> findByMarketMakerIdAndSymbol(String marketMakerId, String symbol);

    List<MarketMakerQuoteStateRecord> findByMarketMakerIdOrderByUpdatedAtDesc(String marketMakerId, Pageable pageable);

    List<MarketMakerQuoteStateRecord> findByActiveTrueOrderByUpdatedAtDesc(Pageable pageable);
}
