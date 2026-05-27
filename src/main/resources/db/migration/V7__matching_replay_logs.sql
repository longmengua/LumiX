-- 檔案用途：SQL migration，建立 durable matching command/event log baseline。
CREATE TABLE IF NOT EXISTS matching_command_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol_code VARCHAR(32) NOT NULL,
    offset_value BIGINT NOT NULL,
    command_type VARCHAR(32) NOT NULL,
    order_payload JSON NOT NULL,
    new_price DECIMAL(38, 18) NULL,
    new_qty DECIMAL(38, 18) NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_matching_command_symbol_offset (symbol_code, offset_value),
    KEY idx_matching_command_symbol_created (symbol_code, created_at)
);

CREATE TABLE IF NOT EXISTS matching_event_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol_code VARCHAR(32) NOT NULL,
    offset_value BIGINT NOT NULL,
    command_offset BIGINT NOT NULL,
    trade_payload JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_matching_event_symbol_offset (symbol_code, offset_value),
    KEY idx_matching_event_command_offset (symbol_code, command_offset),
    KEY idx_matching_event_symbol_created (symbol_code, created_at)
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
