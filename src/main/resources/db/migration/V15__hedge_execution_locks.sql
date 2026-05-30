-- File purpose: durable worker lock for scheduled market-maker hedge execution.
CREATE TABLE hedge_execution_locks
(
    lock_name  VARCHAR(128) NOT NULL,
    owner_id   VARCHAR(128) NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (lock_name)
);
