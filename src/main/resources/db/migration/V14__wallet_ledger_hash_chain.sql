-- File purpose: add tamper-evidence hash-chain columns to wallet ledger journal entries.
ALTER TABLE wallet_ledger_entries
    ADD COLUMN previous_hash VARCHAR(128) NULL,
    ADD COLUMN entry_hash VARCHAR(128) NULL;

CREATE INDEX idx_wallet_ledger_entry_hash
    ON wallet_ledger_entries (entry_hash);
