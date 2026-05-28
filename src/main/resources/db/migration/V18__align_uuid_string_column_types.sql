ALTER TABLE wallet_ledger_postings
    DROP FOREIGN KEY fk_wallet_ledger_postings_entry;

ALTER TABLE reconciliation_report_issues
    DROP FOREIGN KEY fk_reconciliation_report_issues_report;

ALTER TABLE outbox_events
    MODIFY id VARCHAR(36) NOT NULL;

ALTER TABLE dlq_events
    MODIFY id VARCHAR(36) NOT NULL,
    MODIFY outbox_id VARCHAR(36) NULL;

ALTER TABLE order_lifecycle_events
    MODIFY order_id VARCHAR(36) NOT NULL;

ALTER TABLE order_lifecycle_projection
    MODIFY order_id VARCHAR(36) NOT NULL;

ALTER TABLE wallet_ledger_entries
    MODIFY id VARCHAR(36) NOT NULL;

ALTER TABLE wallet_ledger_postings
    MODIFY entry_id VARCHAR(36) NOT NULL;

ALTER TABLE reconciliation_reports
    MODIFY id VARCHAR(36) NOT NULL;

ALTER TABLE reconciliation_report_issues
    MODIFY report_id VARCHAR(36) NOT NULL;

ALTER TABLE turnover_records
    MODIFY id VARCHAR(36) NOT NULL,
    MODIFY order_id VARCHAR(36) NOT NULL;

ALTER TABLE bonus_credit_grants
    MODIFY id VARCHAR(36) NOT NULL;

ALTER TABLE hedge_decision_audits
    MODIFY id VARCHAR(36) NOT NULL;

ALTER TABLE hedge_fills
    MODIFY id VARCHAR(36) NOT NULL;

ALTER TABLE wallet_ledger_postings
    ADD CONSTRAINT fk_wallet_ledger_postings_entry
        FOREIGN KEY (entry_id) REFERENCES wallet_ledger_entries (id);

ALTER TABLE reconciliation_report_issues
    ADD CONSTRAINT fk_reconciliation_report_issues_report
        FOREIGN KEY (report_id) REFERENCES reconciliation_reports (id);
