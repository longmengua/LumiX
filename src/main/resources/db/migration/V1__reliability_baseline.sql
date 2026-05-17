CREATE TABLE IF NOT EXISTS outbox_events (
    id CHAR(36) PRIMARY KEY,
    topic VARCHAR(128) NOT NULL,
    event_key VARCHAR(256) NOT NULL,
    event_type VARCHAR(256) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_error TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    next_attempt_at TIMESTAMP(6) NOT NULL,
    published_at TIMESTAMP(6) NULL,
    KEY idx_outbox_due (status, next_attempt_at),
    KEY idx_outbox_topic_key (topic, event_key)
);

CREATE TABLE IF NOT EXISTS dlq_events (
    id CHAR(36) PRIMARY KEY,
    outbox_id CHAR(36) NULL,
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
