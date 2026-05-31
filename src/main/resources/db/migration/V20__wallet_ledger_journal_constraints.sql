-- File purpose: tighten wallet ledger journal invariants that SQL can enforce directly.
ALTER TABLE wallet_ledger_entries
    ADD CONSTRAINT chk_wallet_ledger_entry_schema_version_positive CHECK (schema_version > 0),
    ADD CONSTRAINT chk_wallet_ledger_entry_asset_not_blank CHECK (CHAR_LENGTH(TRIM(asset)) > 0),
    ADD CONSTRAINT chk_wallet_ledger_entry_reason_not_blank CHECK (CHAR_LENGTH(TRIM(reason)) > 0);

ALTER TABLE wallet_ledger_postings
    ADD CONSTRAINT chk_wallet_ledger_posting_line_positive CHECK (line_no > 0),
    ADD CONSTRAINT chk_wallet_ledger_posting_account_not_blank CHECK (CHAR_LENGTH(TRIM(account_code)) > 0),
    ADD CONSTRAINT chk_wallet_ledger_posting_asset_not_blank CHECK (CHAR_LENGTH(TRIM(asset)) > 0);
