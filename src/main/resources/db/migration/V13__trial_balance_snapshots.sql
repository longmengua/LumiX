-- File purpose: persist daily trial balance snapshots for finance close and audit replay.
CREATE TABLE trial_balance_snapshots
(
    id            VARCHAR(36)    NOT NULL,
    report_date   DATE           NOT NULL,
    uid           BIGINT         NOT NULL,
    asset         VARCHAR(32)    NOT NULL,
    total_debit   DECIMAL(38, 18) NOT NULL,
    total_credit  DECIMAL(38, 18) NOT NULL,
    balanced      BOOLEAN        NOT NULL,
    generated_at  DATETIME(6)    NOT NULL,
    lines_payload JSON           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_trial_balance_snapshot_scope UNIQUE (report_date, uid, asset)
);

CREATE INDEX idx_trial_balance_snapshot_date
    ON trial_balance_snapshots (report_date, uid);
