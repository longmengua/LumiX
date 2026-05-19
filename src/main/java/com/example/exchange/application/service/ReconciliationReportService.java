/*
 * 檔案用途：應用服務，產生、保存與查詢 reconciliation report。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.ReconciliationReportResult;
import com.example.exchange.domain.model.dto.ValidationIssue;
import com.example.exchange.domain.model.entity.ReconciliationReport;
import com.example.exchange.domain.model.entity.ReconciliationReportIssue;
import com.example.exchange.domain.repository.ReconciliationReportStore;
import com.example.exchange.infra.config.ReconciliationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationReportService {

    public static final int SCHEMA_VERSION = 1;
    public static final String REPORT_TYPE = "ACCOUNT_RECONCILIATION";

    private final ReconciliationService reconciliationService;
    private final ReconciliationReportStore reportStore;
    private final ReconciliationProperties properties;

    @Transactional
    public ReconciliationReportResult runAndPersist(String triggeredBy) {
        Instant startedAt = Instant.now();
        List<ValidationIssue> issues = reconciliationService.validateAllAccounts();
        Instant completedAt = Instant.now();

        int errorCount = countSeverity(issues, "ERROR");
        int warnCount = countSeverity(issues, "WARN");
        ReconciliationReport report = ReconciliationReport.create(
                SCHEMA_VERSION,
                REPORT_TYPE,
                normalizeTrigger(triggeredBy),
                reconciliationService.discoverAccountUids().size(),
                issues.size(),
                errorCount,
                warnCount,
                properties.getAlertRoute(),
                startedAt,
                completedAt
        );

        AtomicInteger lineNo = new AtomicInteger(1);
        List<ReconciliationReportIssue> records = issues.stream()
                .map(issue -> ReconciliationReportIssue.from(report.getId(), lineNo.getAndIncrement(), issue))
                .toList();
        reportStore.save(report, records);

        if (properties.isAlertOnError() && errorCount > 0) {
            log.error(
                    "RECONCILIATION_ALERT route={} reportId={} status={} errors={} warnings={} issues={}",
                    properties.getAlertRoute(),
                    report.getId(),
                    report.getStatus(),
                    errorCount,
                    warnCount,
                    issues.size()
            );
        }

        return ReconciliationReportResult.from(report, records);
    }

    @Transactional(readOnly = true)
    public Optional<ReconciliationReportResult> findById(String reportId) {
        if (reportId == null || reportId.isBlank()) return Optional.empty();
        return reportStore.findById(reportId.trim())
                .map(report -> ReconciliationReportResult.from(report, reportStore.findIssues(report.getId())));
    }

    @Transactional(readOnly = true)
    public List<ReconciliationReportResult> latest(int limit) {
        return reportStore.latest(Math.max(1, limit))
                .stream()
                .map(report -> ReconciliationReportResult.from(report, reportStore.findIssues(report.getId())))
                .toList();
    }

    private static int countSeverity(List<ValidationIssue> issues, String severity) {
        return (int) issues.stream()
                .filter(issue -> severity.equalsIgnoreCase(issue.severity()))
                .count();
    }

    private static String normalizeTrigger(String triggeredBy) {
        return triggeredBy == null || triggeredBy.isBlank() ? "MANUAL" : triggeredBy.trim().toUpperCase();
    }
}
