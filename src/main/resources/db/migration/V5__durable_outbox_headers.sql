-- 檔案用途：SQL migration，補齊 production durable outbox headers 與 manual compensation 相容欄位。
ALTER TABLE outbox_events
    ADD COLUMN headers JSON NULL AFTER payload,
    MODIFY next_attempt_at TIMESTAMP(6) NULL;
