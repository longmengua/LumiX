-- 檔案用途：SQL migration，建立 core-v1 乾淨 baseline schema。
CREATE TABLE IF NOT EXISTS outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    topic VARCHAR(128) NOT NULL,
    event_key VARCHAR(256) NOT NULL,
    event_type VARCHAR(256) NOT NULL,
    payload JSON NOT NULL,
    headers JSON NULL,
    status VARCHAR(32) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_error TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    next_attempt_at TIMESTAMP(6) NULL,
    published_at TIMESTAMP(6) NULL,
    KEY idx_outbox_due (status, next_attempt_at),
    KEY idx_outbox_topic_key (topic, event_key)
);

CREATE TABLE IF NOT EXISTS dlq_events (
    id VARCHAR(36) PRIMARY KEY,
    outbox_id VARCHAR(36) NULL,
    topic VARCHAR(128) NOT NULL,
    event_key VARCHAR(256) NOT NULL,
    event_type VARCHAR(256) NOT NULL,
    payload JSON NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    error TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_dlq_created_at (created_at),
    KEY idx_dlq_topic_key (topic, event_key)
);

CREATE TABLE IF NOT EXISTS idempotency_keys (
    idem_key VARCHAR(512) PRIMARY KEY,
    expires_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_idempotency_expires_at (expires_at)
);

CREATE TABLE IF NOT EXISTS snapshots (
    uid BIGINT NOT NULL,
    last_event_seq BIGINT NOT NULL,
    aggregates JSON NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (uid, last_event_seq),
    KEY idx_snapshots_uid_created (uid, created_at)
);

CREATE TABLE IF NOT EXISTS order_lifecycle_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schema_version INT NOT NULL,
    order_id VARCHAR(36) NOT NULL,
    uid BIGINT NOT NULL,
    symbol VARCHAR(64) NOT NULL,
    client_order_id VARCHAR(128) NULL,
    stage VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason_code VARCHAR(128) NULL,
    price DECIMAL(38, 18) NULL,
    orig_qty DECIMAL(38, 18) NULL,
    remaining_qty DECIMAL(38, 18) NULL,
    executed_qty DECIMAL(38, 18) NULL,
    avg_price DECIMAL(38, 18) NULL,
    event_ts DATETIME(6) NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    KEY idx_order_lifecycle_order (order_id, event_ts),
    KEY idx_order_lifecycle_uid_symbol (uid, symbol, event_ts),
    KEY idx_order_lifecycle_stage_ts (stage, event_ts),
    KEY idx_order_lifecycle_client_order (client_order_id)
);

CREATE TABLE IF NOT EXISTS order_lifecycle_projection (
    order_id VARCHAR(36) PRIMARY KEY,
    schema_version INT NOT NULL,
    uid BIGINT NOT NULL,
    symbol VARCHAR(64) NOT NULL,
    client_order_id VARCHAR(128) NULL,
    latest_stage VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason_code VARCHAR(128) NULL,
    price DECIMAL(38, 18) NULL,
    orig_qty DECIMAL(38, 18) NULL,
    remaining_qty DECIMAL(38, 18) NULL,
    executed_qty DECIMAL(38, 18) NULL,
    avg_price DECIMAL(38, 18) NULL,
    first_event_at DATETIME(6) NOT NULL,
    last_event_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_order_lifecycle_projection_uid_symbol (uid, symbol, last_event_at),
    KEY idx_order_lifecycle_projection_status (status, last_event_at),
    KEY idx_order_lifecycle_projection_client_order (client_order_id)
);

CREATE TABLE IF NOT EXISTS wallet_ledger_entries (
    id VARCHAR(36) PRIMARY KEY,
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
    entry_id VARCHAR(36) NOT NULL,
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

CREATE TABLE IF NOT EXISTS reconciliation_reports (
    id VARCHAR(36) PRIMARY KEY,
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
    report_id VARCHAR(36) NOT NULL,
    line_no INT NOT NULL,
    severity VARCHAR(16) NOT NULL,
    code VARCHAR(128) NOT NULL,
    message TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    owner VARCHAR(128) NULL,
    resolved_at DATETIME(6) NULL,
    UNIQUE KEY uk_reconciliation_report_issue_line (report_id, line_no),
    KEY idx_reconciliation_report_issues_report (report_id),
    KEY idx_reconciliation_report_issues_code (code),
    KEY idx_reconciliation_report_issues_severity (severity),
    KEY idx_reconciliation_report_issues_status (status, created_at),
    KEY idx_reconciliation_report_issues_owner (owner, status),
    CONSTRAINT fk_reconciliation_report_issues_report
        FOREIGN KEY (report_id) REFERENCES reconciliation_reports (id)
);

CREATE TABLE IF NOT EXISTS account_risk_snapshots (
    id VARCHAR(36) PRIMARY KEY,
    schema_version INT NOT NULL,
    uid BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    cross_balance DECIMAL(38, 18) NOT NULL,
    available_balance DECIMAL(38, 18) NOT NULL,
    order_hold DECIMAL(38, 18) NOT NULL,
    position_margin DECIMAL(38, 18) NOT NULL,
    frozen_funds DECIMAL(38, 18) NOT NULL,
    unrealized_pnl DECIMAL(38, 18) NOT NULL,
    total_equity DECIMAL(38, 18) NOT NULL,
    maintenance_margin DECIMAL(38, 18) NOT NULL,
    risk_ratio DECIMAL(38, 18) NOT NULL,
    open_position_count INT NOT NULL,
    calculated_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    KEY idx_account_risk_snapshots_uid_day (uid, snapshot_date),
    KEY idx_account_risk_snapshots_uid_time (uid, calculated_at),
    CONSTRAINT chk_account_risk_snapshot_open_positions CHECK (open_position_count >= 0)
);

CREATE TABLE IF NOT EXISTS matching_command_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol_code VARCHAR(32) NOT NULL,
    offset_value BIGINT NOT NULL,
    command_type VARCHAR(32) NOT NULL,
    order_payload JSON NOT NULL,
    replacement_order_payload JSON NULL,
    new_price DECIMAL(38, 18) NULL,
    new_qty DECIMAL(38, 18) NULL,
    owner_id VARCHAR(128) NULL,
    owner_epoch BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_matching_command_symbol_offset (symbol_code, offset_value),
    KEY idx_matching_command_symbol_created (symbol_code, created_at),
    KEY idx_matching_command_owner_epoch (symbol_code, owner_id, owner_epoch)
);

CREATE TABLE IF NOT EXISTS matching_event_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol_code VARCHAR(32) NOT NULL,
    offset_value BIGINT NOT NULL,
    command_offset BIGINT NOT NULL,
    trade_payload JSON NOT NULL,
    owner_id VARCHAR(128) NULL,
    owner_epoch BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_matching_event_symbol_offset (symbol_code, offset_value),
    KEY idx_matching_event_command_offset (symbol_code, command_offset),
    KEY idx_matching_event_symbol_created (symbol_code, created_at),
    KEY idx_matching_event_owner_epoch (symbol_code, owner_id, owner_epoch)
);

CREATE TABLE IF NOT EXISTS matching_offset_checkpoints (
    symbol_code VARCHAR(32) PRIMARY KEY,
    command_offset BIGINT NOT NULL DEFAULT 0,
    event_offset BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS matching_engine_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol_code VARCHAR(32) NOT NULL,
    match_sequence BIGINT NOT NULL,
    command_offset BIGINT NOT NULL,
    event_offset BIGINT NOT NULL,
    snapshot_payload JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    KEY idx_matching_snapshot_latest (symbol_code, command_offset, event_offset, created_at)
);

CREATE TABLE IF NOT EXISTS matching_replay_validation_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol_code VARCHAR(32) NOT NULL,
    valid BOOLEAN NOT NULL,
    start_command_offset BIGINT NOT NULL,
    expected_command_offset BIGINT NOT NULL,
    actual_command_offset BIGINT NOT NULL,
    expected_event_offset BIGINT NOT NULL,
    actual_event_offset BIGINT NOT NULL,
    expected_match_sequence BIGINT NOT NULL,
    actual_match_sequence BIGINT NOT NULL,
    issues_payload JSON NOT NULL,
    validated_at DATETIME(6) NOT NULL,
    KEY idx_matching_replay_report_symbol_time (symbol_code, validated_at),
    KEY idx_matching_replay_report_valid (valid, validated_at)
);

CREATE TABLE IF NOT EXISTS matching_sequencer_leases (
    symbol_code VARCHAR(32) PRIMARY KEY,
    owner_id VARCHAR(128) NOT NULL,
    epoch BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    command_offset BIGINT NOT NULL DEFAULT 0,
    event_offset BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_matching_sequencer_owner (owner_id, expires_at),
    KEY idx_matching_sequencer_expiry (expires_at)
);

CREATE TABLE IF NOT EXISTS turnover_records (
    id VARCHAR(36) PRIMARY KEY,
    schema_version INT NOT NULL,
    uid BIGINT NOT NULL,
    account_id VARCHAR(64) NULL,
    symbol VARCHAR(32) NOT NULL,
    strategy_id VARCHAR(128) NULL,
    market_maker_id VARCHAR(128) NULL,
    order_id VARCHAR(36) NOT NULL,
    match_id VARCHAR(128) NULL,
    trade_seq BIGINT NOT NULL,
    quantity DECIMAL(38, 18) NOT NULL,
    price DECIMAL(38, 18) NOT NULL,
    notional DECIMAL(38, 18) NOT NULL,
    traded_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_turnover_trade_order (trade_seq, order_id),
    KEY idx_turnover_uid_created (uid, created_at),
    KEY idx_turnover_symbol_created (symbol, created_at),
    KEY idx_turnover_strategy (uid, strategy_id, created_at),
    KEY idx_turnover_market_maker (market_maker_id, created_at),
    KEY idx_turnover_match (match_id),
    CONSTRAINT chk_turnover_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT chk_turnover_price_non_negative CHECK (price >= 0),
    CONSTRAINT chk_turnover_notional_non_negative CHECK (notional >= 0)
);

CREATE TABLE IF NOT EXISTS bonus_credit_grants (
    id VARCHAR(36) PRIMARY KEY,
    schema_version INT NOT NULL,
    uid BIGINT NOT NULL,
    asset VARCHAR(32) NOT NULL,
    original_amount DECIMAL(38, 18) NOT NULL,
    remaining_amount DECIMAL(38, 18) NOT NULL,
    campaign_id VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    granted_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_bonus_credit_uid_asset_status (uid, asset, status, expires_at),
    KEY idx_bonus_credit_expiry (status, expires_at),
    KEY idx_bonus_credit_campaign (campaign_id),
    CONSTRAINT chk_bonus_credit_original_non_negative CHECK (original_amount >= 0),
    CONSTRAINT chk_bonus_credit_remaining_non_negative CHECK (remaining_amount >= 0)
);

CREATE TABLE IF NOT EXISTS market_maker_profiles (
    market_maker_id VARCHAR(128) PRIMARY KEY,
    schema_version INT NOT NULL,
    uid BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    KEY idx_market_maker_profiles_uid (uid),
    KEY idx_market_maker_profiles_enabled (enabled),
    CONSTRAINT uk_market_maker_profiles_uid UNIQUE (uid)
);

CREATE TABLE IF NOT EXISTS market_maker_risk_limits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    market_maker_id VARCHAR(128) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    max_long_notional DECIMAL(38, 18) NOT NULL DEFAULT 0,
    max_short_notional DECIMAL(38, 18) NOT NULL DEFAULT 0,
    max_order_notional DECIMAL(38, 18) NOT NULL DEFAULT 0,
    max_slippage_rate DECIMAL(38, 18) NOT NULL DEFAULT 0,
    kill_switch BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uk_market_maker_risk_limit_symbol (market_maker_id, symbol),
    KEY idx_market_maker_risk_limits_mm (market_maker_id),
    CONSTRAINT fk_market_maker_risk_limits_profile
        FOREIGN KEY (market_maker_id) REFERENCES market_maker_profiles (market_maker_id),
    CONSTRAINT chk_market_maker_risk_limit_non_negative CHECK (
        max_long_notional >= 0
        AND max_short_notional >= 0
        AND max_order_notional >= 0
        AND max_slippage_rate >= 0
    )
);

CREATE TABLE IF NOT EXISTS hedge_decision_audits (
    id VARCHAR(36) PRIMARY KEY,
    schema_version INT NOT NULL,
    market_maker_id VARCHAR(128) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    accepted BOOLEAN NOT NULL,
    reason VARCHAR(128) NULL,
    order_notional DECIMAL(38, 18) NOT NULL,
    venue_order_id VARCHAR(128) NULL,
    ref_id VARCHAR(128) NULL,
    decided_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    KEY idx_hedge_decision_mm_time (market_maker_id, decided_at),
    KEY idx_hedge_decision_symbol_time (symbol, decided_at),
    KEY idx_hedge_decision_ref (ref_id),
    KEY idx_hedge_decision_accepted (accepted, decided_at),
    CONSTRAINT chk_hedge_decision_notional_non_negative CHECK (order_notional >= 0)
);

CREATE TABLE IF NOT EXISTS hedge_fills (
    id VARCHAR(36) PRIMARY KEY,
    schema_version INT NOT NULL,
    market_maker_id VARCHAR(128) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    venue_order_id VARCHAR(128) NOT NULL,
    venue_fill_id VARCHAR(128) NOT NULL,
    side VARCHAR(16) NOT NULL,
    quantity DECIMAL(38, 18) NOT NULL,
    price DECIMAL(38, 18) NOT NULL,
    fee DECIMAL(38, 18) NOT NULL DEFAULT 0,
    fee_asset VARCHAR(32) NULL,
    ref_id VARCHAR(128) NULL,
    filled_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_hedge_fills_venue_fill (venue_order_id, venue_fill_id),
    KEY idx_hedge_fills_mm_time (market_maker_id, filled_at),
    KEY idx_hedge_fills_venue_order (venue_order_id),
    KEY idx_hedge_fills_ref (ref_id),
    KEY idx_hedge_fills_symbol_time (symbol, filled_at),
    CONSTRAINT chk_hedge_fills_non_negative CHECK (
        quantity > 0
        AND price > 0
        AND fee >= 0
    )
);

CREATE TABLE IF NOT EXISTS polymarket_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    user_address VARCHAR(64) NOT NULL,
    session_signer_address VARCHAR(64) NOT NULL,
    session_private_key VARCHAR(512) NOT NULL,
    typed_data LONGTEXT NOT NULL,
    signature LONGTEXT NULL,
    status VARCHAR(32) NOT NULL,
    issued_at BIGINT NULL,
    expires_at BIGINT NULL,
    created_at VARCHAR(64) NOT NULL,
    confirmed_at VARCHAR(64) NULL,
    revoked_at VARCHAR(64) NULL,
    last_used_at VARCHAR(64) NULL,
    max_order_usdt DECIMAL(38, 18) NULL,
    daily_limit_usdt DECIMAL(38, 18) NULL,
    daily_used_usdt DECIMAL(38, 18) NULL,
    daily_reset_date VARCHAR(16) NULL,
    revoked_reason VARCHAR(256) NULL,
    UNIQUE KEY idx_session_id (session_id),
    KEY idx_user_address (user_address),
    KEY idx_status (status),
    KEY idx_expires_at (expires_at)
);

CREATE TABLE IF NOT EXISTS prediction_market_sync_key (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_slug VARCHAR(128) NULL,
    event_title VARCHAR(256) NULL,
    team_a VARCHAR(128) NOT NULL,
    team_b VARCHAR(128) NOT NULL,
    event_date DATE NOT NULL,
    source VARCHAR(64) NULL,
    sync_enabled BOOLEAN NOT NULL,
    sync_status VARCHAR(32) NULL,
    retry_count INT NULL,
    last_error TEXT NULL,
    last_synced_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_pm_event_slug (event_slug),
    KEY idx_pm_sync_enabled (sync_enabled),
    KEY idx_pm_sync_status (sync_status),
    KEY idx_pm_event_date (event_date)
);

CREATE TABLE IF NOT EXISTS prediction_market_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    market_id VARCHAR(128) NOT NULL,
    event_slug VARCHAR(128) NOT NULL,
    event_title VARCHAR(256) NULL,
    team_a VARCHAR(128) NULL,
    team_b VARCHAR(128) NULL,
    event_date DATE NULL,
    condition_id VARCHAR(128) NULL,
    question VARCHAR(512) NULL,
    market_slug VARCHAR(256) NOT NULL,
    outcome_key VARCHAR(32) NOT NULL,
    outcome_label VARCHAR(128) NULL,
    yes_token_id VARCHAR(256) NULL,
    no_token_id VARCHAR(256) NULL,
    active BOOLEAN NULL,
    closed BOOLEAN NULL,
    accepting_orders BOOLEAN NULL,
    enable_order_book BOOLEAN NULL,
    neg_risk BOOLEAN NULL,
    best_bid DOUBLE NULL,
    best_ask DOUBLE NULL,
    last_trade_price DOUBLE NULL,
    static_yes_price DOUBLE NULL,
    static_no_price DOUBLE NULL,
    no_buy_price DOUBLE NULL,
    no_sell_price DOUBLE NULL,
    liquidity DOUBLE NULL,
    volume DOUBLE NULL,
    volume_24hr DOUBLE NULL,
    outcome_prices TEXT NULL,
    clob_token_ids TEXT NULL,
    last_price_updated_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_pm_market_slug (market_slug),
    KEY idx_pm_info_event_slug (event_slug),
    KEY idx_pm_info_outcome (outcome_key),
    KEY idx_pm_info_price_updated (last_price_updated_at),
    KEY idx_pm_info_active_closed (active, closed),
    CONSTRAINT fk_pm_info_event_slug
        FOREIGN KEY (event_slug) REFERENCES prediction_market_sync_key (event_slug)
);

CREATE TABLE IF NOT EXISTS prediction_market_sync_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(64) NOT NULL,
    last_sync_key_id BIGINT NULL,
    status VARCHAR(32) NOT NULL,
    total_count INT NULL,
    success_count INT NULL,
    failed_count INT NULL,
    last_error TEXT NULL,
    started_at DATETIME(6) NULL,
    finished_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    UNIQUE KEY uk_pm_sync_progress_job (job_name)
);

CREATE TABLE IF NOT EXISTS prediction_polymarket_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    internal_order_id VARCHAR(64) NOT NULL,
    clob_order_id VARCHAR(128) NULL,
    user_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    event_slug VARCHAR(128) NULL,
    market_slug VARCHAR(256) NOT NULL,
    condition_id VARCHAR(128) NULL,
    outcome_key VARCHAR(32) NULL,
    token_id VARCHAR(256) NULL,
    direction VARCHAR(32) NULL,
    side VARCHAR(16) NULL,
    order_type VARCHAR(16) NULL,
    price DECIMAL(38, 18) NULL,
    size DECIMAL(38, 18) NULL,
    usdt_amount DECIMAL(38, 18) NULL,
    status VARCHAR(64) NOT NULL,
    trade_status VARCHAR(64) NULL,
    size_matched DECIMAL(38, 18) NULL,
    last_trade_id VARCHAR(128) NULL,
    last_error TEXT NULL,
    last_clob_payload LONGTEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    last_synced_at DATETIME(6) NULL,
    UNIQUE KEY uk_poly_internal_order_id (internal_order_id),
    KEY idx_poly_order_internal (internal_order_id),
    KEY idx_poly_order_clob (clob_order_id),
    KEY idx_poly_order_status (status),
    KEY idx_poly_order_market (market_slug),
    KEY idx_poly_order_session (session_id)
);

CREATE TABLE IF NOT EXISTS prediction_polymarket_ws_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_key VARCHAR(256) NOT NULL,
    event_type VARCHAR(64) NULL,
    status VARCHAR(64) NULL,
    wallet_address VARCHAR(64) NULL,
    market VARCHAR(128) NULL,
    asset_id VARCHAR(256) NULL,
    order_id VARCHAR(128) NULL,
    trade_id VARCHAR(128) NULL,
    payload LONGTEXT NULL,
    received_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_poly_ws_event_key (event_key),
    KEY idx_poly_ws_event_key (event_key),
    KEY idx_poly_ws_order (order_id),
    KEY idx_poly_ws_trade (trade_id),
    KEY idx_poly_ws_type (event_type)
);
