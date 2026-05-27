/*
 * 檔案用途：Repository contract，定義 matching replay validation report 持久化能力。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.MatchingReplayValidationReport;

import java.util.List;

/**
 * Stores replay validation reports for recovery audit.
 */
public interface MatchingReplayValidationReportStore {

    void save(MatchingReplayValidationReport report);

    List<MatchingReplayValidationReport> findBySymbol(String symbolCode, int limit);
}
