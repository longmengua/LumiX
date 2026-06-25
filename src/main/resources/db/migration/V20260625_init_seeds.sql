-- Squashed Flyway baseline generated for local development schema consolidation.
-- Contains the previous V1..V31 migrations in version order.


-- ============================================================================
-- Source: V1__core_v1_baseline.sql
-- ============================================================================

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


-- ============================================================================
-- Source: V2__adl_execution_records.sql
-- ============================================================================

-- Durable idempotency and audit summary for ADL forced execution commands.
CREATE TABLE IF NOT EXISTS adl_execution_records (
    command_id VARCHAR(128) PRIMARY KEY,
    schema_version INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(128) NOT NULL,
    requested_notional DECIMAL(38, 18) NOT NULL,
    planned_notional DECIMAL(38, 18) NOT NULL,
    executed_notional DECIMAL(38, 18) NOT NULL,
    remaining_notional DECIMAL(38, 18) NOT NULL,
    socialized_loss_charged DECIMAL(38, 18) NOT NULL,
    executed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_adl_execution_status_time (status, updated_at),
    KEY idx_adl_execution_reason_time (reason, updated_at)
);


-- ============================================================================
-- Source: V3__market_data_sequence_checkpoints.sql
-- ============================================================================

-- Durable market-data stream checkpoints for restart recovery and reconnect backfill.
CREATE TABLE IF NOT EXISTS market_data_sequence_checkpoints (
    symbol VARCHAR(32) NOT NULL,
    stream VARCHAR(64) NOT NULL,
    sequence_value BIGINT NOT NULL,
    checksum BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (symbol, stream),
    KEY idx_md_seq_symbol_stream (symbol, stream)
);


-- ============================================================================
-- Source: V4__market_data_depth_deltas.sql
-- ============================================================================

-- Durable depth deltas for reconnect backfill after a known client sequence.
CREATE TABLE IF NOT EXISTS market_data_depth_deltas (
    symbol VARCHAR(32) NOT NULL,
    version_value BIGINT NOT NULL,
    checksum BIGINT NOT NULL,
    bids_json LONGTEXT NOT NULL,
    asks_json LONGTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (symbol, version_value),
    KEY idx_md_depth_symbol_version (symbol, version_value)
);


-- ============================================================================
-- Source: V5__market_data_trade_tape.sql
-- ============================================================================

-- Durable market-data trade tape for restart-safe recent trades.
CREATE TABLE IF NOT EXISTS market_data_trade_tape (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    match_id VARCHAR(128) NOT NULL,
    order_id VARCHAR(36) NULL,
    side VARCHAR(16) NOT NULL,
    price DECIMAL(38, 18) NOT NULL,
    qty DECIMAL(38, 18) NOT NULL,
    maker BOOLEAN NOT NULL,
    trade_ts DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    KEY idx_md_trade_symbol_time (symbol, trade_ts, id),
    KEY idx_md_trade_match (match_id)
);


-- ============================================================================
-- Source: V6__market_data_tickers.sql
-- ============================================================================

-- Durable market ticker latest state for restart-safe top/last/24h fields.
CREATE TABLE IF NOT EXISTS market_data_tickers (
    symbol VARCHAR(32) PRIMARY KEY,
    last_price DECIMAL(38, 18) NULL,
    best_bid DECIMAL(38, 18) NULL,
    best_ask DECIMAL(38, 18) NULL,
    volume_24h DECIMAL(38, 18) NOT NULL,
    high_24h DECIMAL(38, 18) NULL,
    low_24h DECIMAL(38, 18) NULL,
    updated_at DATETIME(6) NOT NULL
);


-- ============================================================================
-- Source: V7__market_data_klines.sql
-- ============================================================================

-- Durable market kline records for restart-safe candlestick queries.
CREATE TABLE IF NOT EXISTS market_data_klines (
    symbol VARCHAR(32) NOT NULL,
    interval_value VARCHAR(16) NOT NULL,
    open_time DATETIME(6) NOT NULL,
    open_price DECIMAL(38, 18) NOT NULL,
    high_price DECIMAL(38, 18) NOT NULL,
    low_price DECIMAL(38, 18) NOT NULL,
    close_price DECIMAL(38, 18) NOT NULL,
    volume DECIMAL(38, 18) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (symbol, interval_value, open_time),
    KEY idx_md_kline_symbol_interval_time (symbol, interval_value, open_time)
);


-- ============================================================================
-- Source: V8__hedge_venue_idempotency_records.sql
-- ============================================================================

-- Durable claim/result store for hedge venue effectful submit idempotency.
CREATE TABLE IF NOT EXISTS hedge_venue_idempotency_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ref_id VARCHAR(128) NOT NULL,
    fingerprint VARCHAR(512) NOT NULL,
    completed BOOLEAN NOT NULL,
    accepted BOOLEAN NULL,
    venue_order_id VARCHAR(128) NULL,
    reason VARCHAR(256) NULL,
    retryable BOOLEAN NULL,
    submitted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_hedge_venue_idem_ref_id UNIQUE (ref_id),
    KEY idx_hedge_venue_idem_completed (completed)
);


-- ============================================================================
-- Source: V9__polymarket_clob_command_records.sql
-- ============================================================================

-- Durable command identity for Polymarket CLOB effectful commands.
CREATE TABLE IF NOT EXISTS polymarket_clob_command_record (
    command_id VARCHAR(128) PRIMARY KEY,
    command_type VARCHAR(32) NOT NULL,
    internal_order_id VARCHAR(64) NOT NULL,
    fingerprint VARCHAR(512) NOT NULL,
    completed BOOLEAN NOT NULL,
    result_status VARCHAR(64) NULL,
    last_error TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_poly_clob_command_order (internal_order_id),
    KEY idx_poly_clob_command_type_completed (command_type, completed)
);


-- ============================================================================
-- Source: V10__rpc_transaction_records.sql
-- ============================================================================

-- Durable idempotency and lifecycle tracking for backend-observed RPC transactions.
CREATE TABLE IF NOT EXISTS rpc_transaction_record (
    command_id VARCHAR(128) PRIMARY KEY,
    chain_id VARCHAR(32) NOT NULL,
    transaction_type VARCHAR(64) NOT NULL,
    wallet_address VARCHAR(64) NOT NULL,
    fingerprint VARCHAR(512) NOT NULL,
    tx_hash VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_error TEXT NULL,
    completed BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_rpc_tx_hash (tx_hash),
    KEY idx_rpc_tx_wallet (wallet_address),
    KEY idx_rpc_tx_type_completed (transaction_type, completed),
    KEY idx_rpc_tx_status_updated (status, updated_at)
);


-- ============================================================================
-- Source: V11__adl_queue_entries.sql
-- ============================================================================

-- Durable ADL queue state for liquidation shortfalls and operator claim ownership.
CREATE TABLE IF NOT EXISTS adl_queue_entries (
    liquidation_id VARCHAR(128) PRIMARY KEY,
    uid BIGINT NOT NULL,
    symbol VARCHAR(64) NOT NULL,
    liquidated_side VARCHAR(16) NOT NULL,
    amount DECIMAL(38, 18) NOT NULL,
    status VARCHAR(32) NOT NULL,
    owner VARCHAR(128) NOT NULL,
    claimed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_adl_queue_status_time (status, created_at),
    KEY idx_adl_queue_owner_status (owner, status),
    KEY idx_adl_queue_symbol_status (symbol, status)
);


-- ============================================================================
-- Source: V12__production_query_indexes.sql
-- ============================================================================

-- Production query indexes for operational order, ledger, event, and prediction-order projections.
CREATE INDEX idx_order_lifecycle_event_status_time
    ON order_lifecycle_events (status, event_ts);

CREATE INDEX idx_order_lifecycle_event_uid_client_time
    ON order_lifecycle_events (uid, client_order_id, event_ts);

CREATE INDEX idx_order_lifecycle_projection_uid_status_updated
    ON order_lifecycle_projection (uid, status, updated_at);

CREATE INDEX idx_order_lifecycle_projection_symbol_status_time
    ON order_lifecycle_projection (symbol, status, last_event_at);

CREATE INDEX idx_wallet_ledger_entries_created
    ON wallet_ledger_entries (created_at);

CREATE INDEX idx_wallet_ledger_entries_uid_reason_created
    ON wallet_ledger_entries (uid, reason, created_at);

CREATE INDEX idx_wallet_ledger_entries_asset_created
    ON wallet_ledger_entries (asset, created_at);

CREATE INDEX idx_wallet_ledger_postings_created
    ON wallet_ledger_postings (created_at);

CREATE INDEX idx_wallet_ledger_postings_account_created
    ON wallet_ledger_postings (account_code, created_at);

CREATE INDEX idx_outbox_events_type_created
    ON outbox_events (event_type, created_at);

CREATE INDEX idx_dlq_events_type_created
    ON dlq_events (event_type, created_at);

CREATE INDEX idx_matching_command_logs_type_created
    ON matching_command_logs (command_type, created_at);

CREATE INDEX idx_matching_event_logs_command_created
    ON matching_event_logs (symbol_code, command_offset, created_at);

CREATE INDEX idx_prediction_order_user_status_updated
    ON prediction_polymarket_order (user_id, status, updated_at);

CREATE INDEX idx_prediction_order_market_status_updated
    ON prediction_polymarket_order (market_slug, status, updated_at);

CREATE INDEX idx_prediction_order_event_status_updated
    ON prediction_polymarket_order (event_slug, status, updated_at);

CREATE INDEX idx_prediction_ws_wallet_type_received
    ON prediction_polymarket_ws_event (wallet_address, event_type, received_at);

CREATE INDEX idx_prediction_ws_market_type_received
    ON prediction_polymarket_ws_event (market, event_type, received_at);


-- ============================================================================
-- Source: V13__trial_balance_snapshots.sql
-- ============================================================================

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


-- ============================================================================
-- Source: V14__wallet_ledger_hash_chain.sql
-- ============================================================================

-- File purpose: add tamper-evidence hash-chain columns to wallet ledger journal entries.
ALTER TABLE wallet_ledger_entries
    ADD COLUMN previous_hash VARCHAR(128) NULL,
    ADD COLUMN entry_hash VARCHAR(128) NULL;

CREATE INDEX idx_wallet_ledger_entry_hash
    ON wallet_ledger_entries (entry_hash);


-- ============================================================================
-- Source: V15__hedge_execution_locks.sql
-- ============================================================================

-- File purpose: durable worker lock for scheduled market-maker hedge execution.
CREATE TABLE hedge_execution_locks
(
    lock_name  VARCHAR(128) NOT NULL,
    owner_id   VARCHAR(128) NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (lock_name)
);


-- ============================================================================
-- Source: V16__market_maker_quote_states.sql
-- ============================================================================

-- File purpose: durable active quote state for market-maker quote ownership restore.
CREATE TABLE market_maker_quote_states
(
    id              VARCHAR(192) NOT NULL,
    market_maker_id VARCHAR(128) NOT NULL,
    uid             BIGINT       NOT NULL,
    symbol          VARCHAR(32)  NOT NULL,
    ref_id          VARCHAR(128),
    active          BOOLEAN      NOT NULL,
    accepted        BOOLEAN      NOT NULL,
    reason          VARCHAR(256),
    canceled_count  INT          NOT NULL,
    bid_order_id    VARCHAR(36),
    ask_order_id    VARCHAR(36),
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_mm_quote_states_mm_symbol UNIQUE (market_maker_id, symbol)
);

CREATE INDEX idx_mm_quote_states_mm_updated ON market_maker_quote_states (market_maker_id, updated_at);
CREATE INDEX idx_mm_quote_states_active_updated ON market_maker_quote_states (active, updated_at);


-- ============================================================================
-- Source: V17__market_maker_quote_state_versions.sql
-- ============================================================================

-- File purpose: per-side quote version and replacement metadata for market-maker quote state.
ALTER TABLE market_maker_quote_states
    ADD COLUMN bid_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN ask_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN replaced_bid_order_id VARCHAR(36),
    ADD COLUMN replaced_ask_order_id VARCHAR(36);


-- ============================================================================
-- Source: V18__hedge_trade_ledger_refs.sql
-- ============================================================================

ALTER TABLE hedge_decision_audits
    ADD COLUMN internal_trade_ref_id VARCHAR(128);

CREATE INDEX idx_hedge_decision_trade_ref
    ON hedge_decision_audits (internal_trade_ref_id);

ALTER TABLE hedge_fills
    ADD COLUMN ledger_ref_id VARCHAR(128);

CREATE INDEX idx_hedge_fills_ledger_ref
    ON hedge_fills (ledger_ref_id);


-- ============================================================================
-- Source: V19__order_strategy_market_maker_tags.sql
-- ============================================================================

ALTER TABLE order_lifecycle_events
    ADD COLUMN strategy_id VARCHAR(128),
    ADD COLUMN market_maker_id VARCHAR(128);

CREATE INDEX idx_order_lifecycle_strategy
    ON order_lifecycle_events (uid, strategy_id, event_ts);

CREATE INDEX idx_order_lifecycle_market_maker
    ON order_lifecycle_events (market_maker_id, event_ts);

ALTER TABLE order_lifecycle_projection
    ADD COLUMN strategy_id VARCHAR(128),
    ADD COLUMN market_maker_id VARCHAR(128);

CREATE INDEX idx_order_lifecycle_projection_strategy
    ON order_lifecycle_projection (uid, strategy_id, last_event_at);

CREATE INDEX idx_order_lifecycle_projection_market_maker
    ON order_lifecycle_projection (market_maker_id, last_event_at);


-- ============================================================================
-- Source: V20__wallet_ledger_journal_constraints.sql
-- ============================================================================

-- File purpose: tighten wallet ledger journal invariants that SQL can enforce directly.
ALTER TABLE wallet_ledger_entries
    ADD CONSTRAINT chk_wallet_ledger_entry_schema_version_positive CHECK (schema_version > 0),
    ADD CONSTRAINT chk_wallet_ledger_entry_asset_not_blank CHECK (CHAR_LENGTH(TRIM(asset)) > 0),
    ADD CONSTRAINT chk_wallet_ledger_entry_reason_not_blank CHECK (CHAR_LENGTH(TRIM(reason)) > 0);

ALTER TABLE wallet_ledger_postings
    ADD CONSTRAINT chk_wallet_ledger_posting_line_positive CHECK (line_no > 0),
    ADD CONSTRAINT chk_wallet_ledger_posting_account_not_blank CHECK (CHAR_LENGTH(TRIM(account_code)) > 0),
    ADD CONSTRAINT chk_wallet_ledger_posting_asset_not_blank CHECK (CHAR_LENGTH(TRIM(asset)) > 0);


-- ============================================================================
-- Source: V21__insurance_fund_movements.sql
-- ============================================================================

-- Durable insurance fund capital movement records for liquidation/ADL operations.
CREATE TABLE IF NOT EXISTS insurance_fund_movements (
    movement_id VARCHAR(128) PRIMARY KEY,
    asset VARCHAR(32) NOT NULL,
    reason VARCHAR(128) NOT NULL,
    ref_id VARCHAR(128) NOT NULL,
    amount DECIMAL(38, 18) NOT NULL,
    balance_after DECIMAL(38, 18) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    KEY idx_insurance_fund_asset_time (asset, created_at),
    KEY idx_insurance_fund_reason_time (reason, created_at),
    KEY idx_insurance_fund_ref (ref_id)
);


-- ============================================================================
-- Source: V22__polymarket_user_ws_checkpoints.sql
-- ============================================================================

-- Durable checkpoint for the Polymarket authenticated user WebSocket gateway.
CREATE TABLE IF NOT EXISTS prediction_polymarket_user_ws_checkpoint (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stream_key VARCHAR(160) NOT NULL,
    wallet_address VARCHAR(64) NULL,
    last_event_key VARCHAR(256) NULL,
    last_event_type VARCHAR(64) NULL,
    last_received_at DATETIME(6) NULL,
    last_payload LONGTEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_poly_user_ws_checkpoint_stream (stream_key),
    KEY idx_poly_user_ws_checkpoint_wallet (wallet_address),
    KEY idx_poly_user_ws_checkpoint_received (last_received_at)
);


-- ============================================================================
-- Source: V23__position_lifecycle_projection.sql
-- ============================================================================

-- Durable live-position SQL mirror schema and production query indexes.
CREATE TABLE IF NOT EXISTS position_lifecycle_projection (
    uid BIGINT NOT NULL,
    symbol VARCHAR(64) NOT NULL,
    schema_version INT NOT NULL DEFAULT 1,
    mode VARCHAR(32) NOT NULL,
    leverage DECIMAL(38, 18) NOT NULL DEFAULT 1.000000000000000000,
    qty DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000,
    entry_price DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000,
    margin DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000,
    realized_pnl DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000,
    fee_paid DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000,
    rebate_earned DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000,
    funding_paid DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000,
    funding_received DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000,
    insurance_fund_covered DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000,
    adl_covered DECIMAL(38, 18) NOT NULL DEFAULT 0.000000000000000000,
    last_trade_ref VARCHAR(128),
    updated_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (uid, symbol),
    KEY idx_position_projection_symbol_qty_updated (symbol, qty, updated_at),
    KEY idx_position_projection_uid_updated (uid, updated_at),
    KEY idx_position_projection_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================================
-- Source: V24__local_auth_users.sql
-- ============================================================================

-- Local first-party user authentication tables.
-- These tables intentionally store password hashes and refresh-token hashes only; raw secrets stay out of SQL.
CREATE TABLE IF NOT EXISTS app_users (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Internal user id; also used as exchange account uid.',
    email VARCHAR(320) NOT NULL COMMENT 'Normalized lowercase first-party login identifier.',
    password_hash VARCHAR(255) NOT NULL COMMENT 'PBKDF2 password hash with algorithm metadata and salt.',
    status VARCHAR(32) NOT NULL COMMENT 'User lifecycle state such as ACTIVE or future disabled states.',
    roles VARCHAR(255) NOT NULL COMMENT 'Space-delimited roles copied into issued JWT claims.',
    scopes VARCHAR(255) NOT NULL COMMENT 'Space-delimited scopes copied into issued JWT claims.',
    created_at DATETIME(6) NOT NULL COMMENT 'Registration timestamp.',
    updated_at DATETIME(6) NOT NULL COMMENT 'Last profile/status update timestamp.',
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_users_email (email),
    KEY idx_app_users_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='First-party exchange users for local registration and login.';

-- Refresh sessions make logout/revocation server-side while access JWTs remain short-lived and stateless.
CREATE TABLE IF NOT EXISTS auth_refresh_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Refresh session primary key.',
    user_id BIGINT NOT NULL COMMENT 'Owner app_users.id / exchange uid.',
    refresh_token_hash VARCHAR(64) NOT NULL COMMENT 'SHA-256 hex of refresh token; raw token is never stored.',
    session_id VARCHAR(64) NOT NULL COMMENT 'Stable session identifier for future device/session management.',
    expires_at DATETIME(6) NOT NULL COMMENT 'Hard expiry for refresh-token reuse.',
    revoked_at DATETIME(6) COMMENT 'Logout/revocation timestamp; null means still active until expiry.',
    created_at DATETIME(6) NOT NULL COMMENT 'Session creation timestamp.',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_refresh_sessions_token_hash (refresh_token_hash),
    KEY idx_auth_refresh_sessions_user_created (user_id, created_at),
    KEY idx_auth_refresh_sessions_expires (expires_at),
    CONSTRAINT fk_auth_refresh_sessions_user
        FOREIGN KEY (user_id) REFERENCES app_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Server-side refresh-token sessions for local exchange auth.';


-- ============================================================================
-- Source: V25__fee_config_change_log.sql
-- ============================================================================

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


-- ============================================================================
-- Source: V26__market_maker_quote_state_prices.sql
-- ============================================================================

-- File purpose: preserve the visible price/quantity for the latest market-maker quote legs so client/admin screens can identify maker liquidity without reverse-engineering order ids.
ALTER TABLE market_maker_quote_states
    ADD COLUMN bid_price DECIMAL(38, 18) COMMENT 'Latest accepted bid quote price for this market-maker/symbol state.',
    ADD COLUMN bid_quantity DECIMAL(38, 18) COMMENT 'Latest accepted bid quote quantity shown in the client order book.',
    ADD COLUMN ask_price DECIMAL(38, 18) COMMENT 'Latest accepted ask quote price for this market-maker/symbol state.',
    ADD COLUMN ask_quantity DECIMAL(38, 18) COMMENT 'Latest accepted ask quote quantity shown in the client order book.';


-- ============================================================================
-- Source: V27__customer_auth_email_verification.sql
-- ============================================================================

-- Customer-auth hardening: email verification state for first-party users.
-- Existing ACTIVE users are treated as already verified so upgrades do not lock out current accounts.
ALTER TABLE app_users
    ADD COLUMN email_verified_at DATETIME(6) COMMENT 'Mailbox ownership verification timestamp; null means login is blocked when verification is enabled.',
    ADD COLUMN email_verification_token_hash VARCHAR(64) COMMENT 'SHA-256 hash of the pending verification token; raw token is sent only by email.',
    ADD COLUMN email_verification_expires_at DATETIME(6) COMMENT 'Expiry for the pending email verification token.';

UPDATE app_users
SET email_verified_at = created_at
WHERE status = 'ACTIVE' AND email_verified_at IS NULL;

CREATE INDEX idx_app_users_verification_token ON app_users (email_verification_token_hash);


-- ============================================================================
-- Source: V28__customer_registration_requests.sql
-- ============================================================================

-- Pending customer registration requests are decoupled from finalized app_users/accounts.
-- A request expires after its verification window; only VERIFIED requests create app_users rows.
CREATE TABLE IF NOT EXISTS customer_registration_requests (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Pending registration request id, separate from app_users.uid.',
    email VARCHAR(320) NOT NULL COMMENT 'Normalized customer email being registered.',
    password_hash VARCHAR(255) NOT NULL COMMENT 'Password hash promoted to app_users only after verification succeeds.',
    verification_token_hash VARCHAR(64) NOT NULL COMMENT 'SHA-256 hash of link token; raw token is sent by email only.',
    verification_code_hash VARCHAR(64) NOT NULL COMMENT 'SHA-256 hash of the 6-digit email code.',
    status VARCHAR(32) NOT NULL COMMENT 'PENDING, VERIFIED, or EXPIRED.',
    expires_at DATETIME(6) NOT NULL COMMENT 'Registration verification deadline, normally created_at + 24 hours.',
    verified_at DATETIME(6) NULL COMMENT 'When the email code/link completed registration.',
    created_at DATETIME(6) NOT NULL COMMENT 'Registration request creation timestamp used for the one-day validity rule.',
    updated_at DATETIME(6) NOT NULL COMMENT 'Last request state update timestamp.',
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_registration_token (verification_token_hash),
    KEY idx_customer_registration_email_status_created (email, status, created_at),
    KEY idx_customer_registration_status_expires (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================================
-- Source: V29__customer_verification_codes.sql
-- ============================================================================

-- File purpose: decouple customer email verification codes from a single registration feature.
-- Codes are keyed by normalized email and may optionally point at the pending registration or finalized user.
CREATE TABLE IF NOT EXISTS customer_verification_codes (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Verification code id; code rows are independent from app_users uid.',
    email VARCHAR(320) NOT NULL COMMENT 'Normalized customer email the code was issued to.',
    app_user_id BIGINT NULL COMMENT 'Optional finalized app_users.id when the account already exists.',
    registration_request_id BIGINT NULL COMMENT 'Optional pending customer_registration_requests.id for registration activation.',
    code_hash VARCHAR(64) NOT NULL COMMENT 'SHA-256 hash of normalized email plus raw six-digit code; raw code is sent by email only.',
    status VARCHAR(32) NOT NULL COMMENT 'PENDING, VERIFIED, or EXPIRED.',
    expires_at DATETIME(6) NOT NULL COMMENT 'Verification deadline; normally follows the registration request expiry.',
    verified_at DATETIME(6) NULL COMMENT 'When this code was consumed.',
    created_at DATETIME(6) NOT NULL COMMENT 'Creation timestamp used to pick the latest code for manual resend flows.',
    updated_at DATETIME(6) NOT NULL COMMENT 'Last state update timestamp.',
    PRIMARY KEY (id),
    KEY idx_customer_verification_email_status_created (email, status, created_at),
    KEY idx_customer_verification_registration_status (registration_request_id, status),
    KEY idx_customer_verification_user_status (app_user_id, status),
    KEY idx_customer_verification_status_expires (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Pending registrations keep only the backup email-link token and account material; codes live in customer_verification_codes.
ALTER TABLE customer_registration_requests
    DROP COLUMN verification_code_hash;


-- ============================================================================
-- Source: V30__customer_preferred_language.sql
-- ============================================================================

-- Customer preferred language is owned by app_users after registration is completed.
ALTER TABLE app_users
    ADD COLUMN preferred_language VARCHAR(16) NOT NULL DEFAULT 'en'
        COMMENT 'Customer UI/email locale preference: en, zh-TW, ms, or ko.';

-- Pending registrations keep the browser locale before an app_users row exists.
ALTER TABLE customer_registration_requests
    ADD COLUMN preferred_language VARCHAR(16) NOT NULL DEFAULT 'en'
        COMMENT 'Locale captured when the registration request was created, promoted to app_users after verification.';


-- ============================================================================
-- Source: V31__message_center.sql
-- ============================================================================

-- Message center domain tables for per-user state, announcement controls, and notification preferences.
CREATE TABLE message_center_messages
(
    id                  VARCHAR(36)  NOT NULL PRIMARY KEY,
    template_code       VARCHAR(128) NOT NULL DEFAULT '',
    title               VARCHAR(255) NOT NULL COMMENT 'Rendered message title for list and detail views.',
    summary             LONGTEXT     NOT NULL COMMENT 'Short description shown in list and push previews.',
    body                LONGTEXT     NOT NULL COMMENT 'Long message body stored as pure text.',
    category            VARCHAR(32)  NOT NULL,
    severity            VARCHAR(32)  NOT NULL,
    action_url          VARCHAR(512),
    action_label        VARCHAR(128),
    metadata_json       LONGTEXT     NOT NULL COMMENT 'Rendered metadata JSON; application writes {} when empty because MySQL TEXT cannot use defaults.',
    template_vars_json   LONGTEXT     NOT NULL COMMENT 'Template variables JSON; application writes {} when empty because MySQL TEXT cannot use defaults.',
    source_user_id       BIGINT,
    source_event_type    VARCHAR(128),
    source_event_id      VARCHAR(128),
    source_event_hash    VARCHAR(128),
    dedupe_key           VARCHAR(255),
    created_by_subject   VARCHAR(255) NOT NULL,
    created_by_type      VARCHAR(32)  NOT NULL,
    effective_at         DATETIME(6)  NOT NULL,
    created_at           DATETIME(6)  NOT NULL,
    expire_at            DATETIME(6),
    is_scheduled         BOOLEAN      NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    INDEX idx_msg_center_msg_created_id (created_at, id),
    INDEX idx_msg_center_msg_category_created (category, created_at),
    INDEX idx_msg_center_msg_expire (expire_at),
    INDEX idx_msg_center_msg_dedupe (dedupe_key)
) ENGINE = InnoDB COMMENT='Center message template snapshot and send event metadata.';

CREATE TABLE message_center_message_states
(
    uid                  BIGINT       NOT NULL,
    message_id           VARCHAR(36)  NOT NULL,
    is_read              BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at              DATETIME(6),
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    is_archived          BOOLEAN      NOT NULL DEFAULT FALSE,
    is_pinned            BOOLEAN      NOT NULL DEFAULT FALSE,
    pin_at               DATETIME(6),
    last_notified_at     DATETIME(6),
    dedupe_key           VARCHAR(255),
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (uid, message_id),
    INDEX idx_msg_state_uid_category (uid, is_deleted, is_archived, is_read, message_id),
    INDEX idx_msg_state_uid_archived (uid, is_archived, is_deleted, is_read, message_id),
    INDEX idx_msg_state_uid_dedupe (uid, dedupe_key),
    INDEX idx_msg_state_dedupe (dedupe_key),
    CONSTRAINT fk_msg_state_message
        FOREIGN KEY (message_id)
        REFERENCES message_center_messages (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE = InnoDB COMMENT='Per-user read/delete/archive/pin state; supports soft-delete and personalized flags.';

CREATE TABLE message_center_notification_preferences
(
    uid                  BIGINT      NOT NULL,
    category             VARCHAR(32) NOT NULL,
    in_app_enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
    email_enabled        BOOLEAN     NOT NULL DEFAULT TRUE,
    sms_enabled          BOOLEAN     NOT NULL DEFAULT TRUE,
    push_enabled         BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_at           DATETIME(6) NOT NULL,
    updated_by           VARCHAR(255),
    PRIMARY KEY (uid, category),
    INDEX idx_msg_pref_uid_category (uid, category)
) ENGINE = InnoDB COMMENT='Per-user channel preference for each message category.';

CREATE TABLE message_center_announcements
(
    id                    VARCHAR(36) NOT NULL PRIMARY KEY,
    title                 VARCHAR(255) NOT NULL,
    summary               LONGTEXT     NOT NULL,
    category              VARCHAR(32)  NOT NULL,
    severity              VARCHAR(32)  NOT NULL,
    template_code         VARCHAR(128),
    template_vars_json    LONGTEXT     NOT NULL COMMENT 'Template variables JSON; application writes {} when empty because MySQL TEXT cannot use defaults.',
    action_url            VARCHAR(512),
    action_label          VARCHAR(128),
    audience_type         VARCHAR(32)  NOT NULL,
    audience_data         LONGTEXT     NOT NULL COMMENT 'Audience condition JSON; application writes {} when empty because MySQL TEXT cannot use defaults.',
    send_at               DATETIME(6)  NOT NULL,
    expire_at             DATETIME(6),
    status                VARCHAR(32)  NOT NULL,
    delivery_mode         VARCHAR(32)  NOT NULL,
    dedupe_key            VARCHAR(255),
    estimated_recipients  BIGINT       NOT NULL,
    sent_count            BIGINT       NOT NULL,
    failed_count          BIGINT       NOT NULL,
    skipped_count         BIGINT       NOT NULL,
    created_by_subject    VARCHAR(255) NOT NULL,
    created_by_type       VARCHAR(32)  NOT NULL,
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL,
    INDEX idx_msg_announce_status_send_at (status, send_at),
    INDEX idx_msg_announce_category_status (category, status),
    INDEX idx_msg_announce_created_at (created_at),
    INDEX idx_msg_announce_dedupe (dedupe_key)
) ENGINE = InnoDB COMMENT='Announcement authoring artifact before per-user materialization.';

