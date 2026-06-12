-- Customer preferred language is owned by app_users after registration is completed.
ALTER TABLE app_users
    ADD COLUMN preferred_language VARCHAR(16) NOT NULL DEFAULT 'en'
        COMMENT 'Customer UI/email locale preference: en, zh-TW, ms, or ko.';

-- Pending registrations keep the browser locale before an app_users row exists.
ALTER TABLE customer_registration_requests
    ADD COLUMN preferred_language VARCHAR(16) NOT NULL DEFAULT 'en'
        COMMENT 'Locale captured when the registration request was created, promoted to app_users after verification.';
