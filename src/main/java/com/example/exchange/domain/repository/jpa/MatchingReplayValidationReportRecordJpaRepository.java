/*
 * 檔案用途：Spring Data repository，查詢 durable matching replay validation report。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MatchingReplayValidationReportRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchingReplayValidationReportRecordJpaRepository
        extends JpaRepository<MatchingReplayValidationReportRecord, Long> {

    List<MatchingReplayValidationReportRecord> findBySymbolCodeOrderByValidatedAtDesc(String symbolCode, Pageable pageable);
}
