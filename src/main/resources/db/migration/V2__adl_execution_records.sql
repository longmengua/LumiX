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
