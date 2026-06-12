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
