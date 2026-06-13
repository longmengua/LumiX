/*
 * 檔案用途：Spring Data JPA repository，提供做市商 profile 查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MarketMakerProfileRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketMakerProfileRecordJpaRepository extends JpaRepository<MarketMakerProfileRecord, String> {

    Optional<MarketMakerProfileRecord> findByUid(long uid);

    /** Keeps admin profile recovery deterministic by listing disabled and enabled makers in stable order. */
    List<MarketMakerProfileRecord> findAllByOrderByMarketMakerIdAsc();

    List<MarketMakerProfileRecord> findByEnabledTrueOrderByMarketMakerIdAsc();
}
