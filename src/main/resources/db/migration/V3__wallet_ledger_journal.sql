-- 檔案用途：SQL migration，建立 durable double-entry wallet ledger journal。
CREATE TABLE IF NOT EXISTS wallet_ledger_entries (
    id CHAR(36) PRIMARY KEY,
    schema_version INT NOT NULL,
    uid BIGINT NOT NULL,
    asset VARCHAR(32) NOT NULL,
    reason VARCHAR(64) NOT NULL,
    ref_id VARCHAR(128) NULL,
    amount DECIMAL(38, 18) NOT NULL,
    balance_after DECIMAL(38, 18) NULL,
    created_at DATETIME(6) NOT NULL,
    KEY idx_wallet_ledger_uid_asset_created (uid, asset, created_at),
    KEY idx_wallet_ledger_ref (ref_id),
    KEY idx_wallet_ledger_reason_created (reason, created_at),
    CONSTRAINT chk_wallet_ledger_entry_amount_non_negative CHECK (amount >= 0)
);

CREATE TABLE IF NOT EXISTS wallet_ledger_postings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_id CHAR(36) NOT NULL,
    line_no INT NOT NULL,
    account_code VARCHAR(64) NOT NULL,
    asset VARCHAR(32) NOT NULL,
    debit DECIMAL(38, 18) NOT NULL DEFAULT 0,
    credit DECIMAL(38, 18) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_wallet_ledger_posting_line (entry_id, line_no),
    KEY idx_wallet_ledger_posting_entry (entry_id),
    KEY idx_wallet_ledger_posting_account (asset, account_code, created_at),
    CONSTRAINT fk_wallet_ledger_postings_entry
        FOREIGN KEY (entry_id) REFERENCES wallet_ledger_entries (id),
    CONSTRAINT chk_wallet_ledger_posting_non_negative CHECK (debit >= 0 AND credit >= 0),
    CONSTRAINT chk_wallet_ledger_posting_one_side CHECK (
        (debit > 0 AND credit = 0)
        OR (debit = 0 AND credit > 0)
    )
);
