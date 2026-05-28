-- 檔案用途：SQL migration，補 reconciliation issue 狀態與 owner 欄位。
ALTER TABLE reconciliation_report_issues
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    ADD COLUMN owner VARCHAR(128) NULL,
    ADD COLUMN resolved_at DATETIME(6) NULL,
    ADD KEY idx_reconciliation_report_issues_status (status, created_at),
    ADD KEY idx_reconciliation_report_issues_owner (owner, status);
