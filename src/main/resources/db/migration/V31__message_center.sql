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
    metadata_json       LONGTEXT     NOT NULL DEFAULT '{}',
    template_vars_json   LONGTEXT     NOT NULL DEFAULT '{}',
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
    template_vars_json    LONGTEXT     NOT NULL DEFAULT '{}',
    action_url            VARCHAR(512),
    action_label          VARCHAR(128),
    audience_type         VARCHAR(32)  NOT NULL,
    audience_data         LONGTEXT     NOT NULL DEFAULT '{}',
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
