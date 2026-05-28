/*
 * 檔案用途：Repository 介面，定義 persisted reconciliation report 存取契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.entity.ReconciliationReport;
import com.example.exchange.domain.model.entity.ReconciliationReportIssue;

import java.util.List;
import java.util.Optional;

public interface ReconciliationReportStore {

    void save(ReconciliationReport report, List<ReconciliationReportIssue> issues);

    Optional<ReconciliationReport> findById(String reportId);

    List<ReconciliationReportIssue> findIssues(String reportId);

    Optional<ReconciliationReportIssue> findIssue(long issueId);

    void saveIssue(ReconciliationReportIssue issue);

    List<ReconciliationReportIssue> findIssuesByStatus(String status, int limit);

    List<ReconciliationReport> latest(int limit);
}
