/*
 * 檔案用途：JPA adapter，實作 persisted reconciliation report store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.ReconciliationReport;
import com.example.exchange.domain.model.entity.ReconciliationReportIssue;
import com.example.exchange.domain.repository.ReconciliationReportStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaReconciliationReportStore implements ReconciliationReportStore {

    private final ReconciliationReportJpaRepository reportRepository;
    private final ReconciliationReportIssueJpaRepository issueRepository;

    @Override
    @Transactional
    public void save(ReconciliationReport report, List<ReconciliationReportIssue> issues) {
        reportRepository.save(report);
        issueRepository.saveAll(issues);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReconciliationReport> findById(String reportId) {
        return reportRepository.findById(reportId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReconciliationReportIssue> findIssues(String reportId) {
        return issueRepository.findByReportIdOrderByLineNoAsc(reportId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReconciliationReportIssue> findIssue(long issueId) {
        return issueRepository.findById(issueId);
    }

    @Override
    @Transactional
    public void saveIssue(ReconciliationReportIssue issue) {
        issueRepository.save(issue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReconciliationReportIssue> findIssuesByStatus(String status, int limit) {
        return issueRepository.findByStatusOrderByCreatedAtAscIdAsc(status)
                .stream()
                .limit(Math.max(1, limit))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReconciliationReport> latest(int limit) {
        return reportRepository.findTop50ByOrderByCompletedAtDesc()
                .stream()
                .limit(Math.max(1, limit))
                .toList();
    }
}
