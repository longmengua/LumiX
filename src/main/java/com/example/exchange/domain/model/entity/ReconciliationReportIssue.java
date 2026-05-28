/*
 * 檔案用途：JPA entity，保存 reconciliation report issue 明細。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.ValidationIssue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "reconciliation_report_issues",
        indexes = {
                @Index(name = "idx_reconciliation_report_issues_report", columnList = "report_id"),
                @Index(name = "idx_reconciliation_report_issues_code", columnList = "code"),
                @Index(name = "idx_reconciliation_report_issues_severity", columnList = "severity")
        }
)
public class ReconciliationReportIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false, length = 36)
    private String reportId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "severity", nullable = false, length = 16)
    private String severity;

    @Column(name = "code", nullable = false, length = 128)
    private String code;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "owner", length = 128)
    private String owner;

    @Column(name = "resolved_at", columnDefinition = "DATETIME(6)")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    public static ReconciliationReportIssue from(String reportId, int lineNo, ValidationIssue issue) {
        ReconciliationReportIssue record = new ReconciliationReportIssue();
        record.setReportId(reportId);
        record.setLineNo(lineNo);
        record.setSeverity(issue.severity());
        record.setCode(issue.code());
        record.setMessage(issue.message());
        record.setStatus("OPEN");
        record.setCreatedAt(Instant.now());
        return record;
    }

    public ValidationIssue toValidationIssue() {
        return new ValidationIssue(severity, code, message);
    }
}
