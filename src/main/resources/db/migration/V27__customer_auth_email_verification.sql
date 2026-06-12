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
