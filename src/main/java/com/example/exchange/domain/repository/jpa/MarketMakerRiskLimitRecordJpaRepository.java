/*
 * 檔案用途：Spring Data JPA repository，提供做市商 risk limit 查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MarketMakerRiskLimitRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketMakerRiskLimitRecordJpaRepository extends JpaRepository<MarketMakerRiskLimitRecord, Long> {

    List<MarketMakerRiskLimitRecord> findByMarketMakerIdOrderBySymbolAsc(String marketMakerId);

    void deleteByMarketMakerId(String marketMakerId);
}
