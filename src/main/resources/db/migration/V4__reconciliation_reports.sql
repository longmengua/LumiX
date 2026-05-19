-- 檔案用途：SQL migration，建立 persisted reconciliation report 與 issue 明細。
CREATE TABLE IF NOT EXISTS reconciliation_reports (
    id CHAR(36) PRIMARY KEY,
    schema_version INT NOT NULL,
    report_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    triggered_by VARCHAR(32) NOT NULL,
    scanned_accounts INT NOT NULL,
    issue_count INT NOT NULL,
    error_count INT NOT NULL,
    warn_count INT NOT NULL,
    alert_route VARCHAR(128) NULL,
    started_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NOT NULL,
    KEY idx_reconciliation_reports_status (status, completed_at),
    KEY idx_reconciliation_reports_trigger (triggered_by, started_at),
    CONSTRAINT chk_reconciliation_report_counts CHECK (
        scanned_accounts >= 0
        AND issue_count >= 0
        AND error_count >= 0
        AND warn_count >= 0
    )
);

CREATE TABLE IF NOT EXISTS reconciliation_report_issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id CHAR(36) NOT NULL,
    line_no INT NOT NULL,
    severity VARCHAR(16) NOT NULL,
    code VARCHAR(128) NOT NULL,
    message TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_reconciliation_report_issue_line (report_id, line_no),
    KEY idx_reconciliation_report_issues_report (report_id),
    KEY idx_reconciliation_report_issues_code (code),
    KEY idx_reconciliation_report_issues_severity (severity),
    CONSTRAINT fk_reconciliation_report_issues_report
        FOREIGN KEY (report_id) REFERENCES reconciliation_reports (id)
);
