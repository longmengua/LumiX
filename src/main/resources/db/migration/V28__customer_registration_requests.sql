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
