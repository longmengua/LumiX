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
