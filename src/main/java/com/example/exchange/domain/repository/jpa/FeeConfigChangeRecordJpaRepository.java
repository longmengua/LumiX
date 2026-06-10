/*
 * File purpose: JPA repository for querying immutable admin fee-change history.
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.FeeConfigChangeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeConfigChangeRecordJpaRepository extends JpaRepository<FeeConfigChangeRecord, String> {

    /**
     * Lists the newest fee changes first so admin screens can show the latest operational context.
     */
    List<FeeConfigChangeRecord> findTop20BySymbolOrderByChangedAtDesc(String symbol);
}
