-- 檔案用途：SQL migration，補充 matching cancel-replace command 的 replacement order payload。
ALTER TABLE matching_command_logs
    ADD COLUMN replacement_order_payload JSON NULL AFTER order_payload;
