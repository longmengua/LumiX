-- Fee configuration audit history for admin market-fee updates.
-- Runtime fee settings remain in SymbolConfig for the MVP; this table preserves every operator change.
CREATE TABLE IF NOT EXISTS fee_config_change_log (
    id VARCHAR(36) NOT NULL COMMENT 'Immutable fee-change audit id.',
    symbol VARCHAR(32) NOT NULL COMMENT 'Market symbol whose fee schedule changed.',
    old_maker_fee_rate DECIMAL(38, 18) NOT NULL COMMENT 'Maker fee rate before the admin change.',
    old_taker_fee_rate DECIMAL(38, 18) NOT NULL COMMENT 'Taker fee rate before the admin change.',
    new_maker_fee_rate DECIMAL(38, 18) NOT NULL COMMENT 'Maker fee rate after the admin change.',
    new_taker_fee_rate DECIMAL(38, 18) NOT NULL COMMENT 'Taker fee rate after the admin change.',
    operator_id VARCHAR(128) NOT NULL COMMENT 'Admin/operator identity supplied by the protected API caller.',
    reason VARCHAR(512) NOT NULL COMMENT 'Business reason for the fee change.',
    request_id VARCHAR(128) COMMENT 'Optional request/correlation id for audit traceability.',
    effective_at DATETIME(6) NOT NULL COMMENT 'Timestamp when new orders should start using the new fee rates.',
    changed_at DATETIME(6) NOT NULL COMMENT 'Server timestamp when the change was accepted.',
    PRIMARY KEY (id),
    KEY idx_fee_config_symbol_effective (symbol, effective_at),
    KEY idx_fee_config_operator_changed (operator_id, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Append-only admin fee-configuration change log.';
