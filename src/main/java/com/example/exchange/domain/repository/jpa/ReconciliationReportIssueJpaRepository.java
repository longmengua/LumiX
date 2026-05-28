/*
 * 檔案用途：Spring Data JPA repository，提供 reconciliation report issue 存取。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.ReconciliationReportIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconciliationReportIssueJpaRepository
        extends JpaRepository<ReconciliationReportIssue, Long> {

    List<ReconciliationReportIssue> findByReportIdOrderByLineNoAsc(String reportId);

    List<ReconciliationReportIssue> findByStatusOrderByCreatedAtAscIdAsc(String status);
}
