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
