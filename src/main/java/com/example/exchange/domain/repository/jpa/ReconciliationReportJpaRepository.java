/*
 * 檔案用途：Spring Data JPA repository，提供 reconciliation report summary 存取。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.ReconciliationReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconciliationReportJpaRepository
        extends JpaRepository<ReconciliationReport, String> {

    List<ReconciliationReport> findTop50ByOrderByCompletedAtDesc();
}
