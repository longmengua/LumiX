-- 檔案用途：SQL migration，為 matching command/event log 補 owner epoch 審計欄位。
ALTER TABLE matching_command_logs
    ADD COLUMN owner_id VARCHAR(128) NULL AFTER new_qty,
    ADD COLUMN owner_epoch BIGINT NOT NULL DEFAULT 0 AFTER owner_id,
    ADD KEY idx_matching_command_owner_epoch (symbol_code, owner_id, owner_epoch);

ALTER TABLE matching_event_logs
    ADD COLUMN owner_id VARCHAR(128) NULL AFTER trade_payload,
    ADD COLUMN owner_epoch BIGINT NOT NULL DEFAULT 0 AFTER owner_id,
    ADD KEY idx_matching_event_owner_epoch (symbol_code, owner_id, owner_epoch);
