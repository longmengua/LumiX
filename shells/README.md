# shells

這裡放本機操作腳本與 API curl 範例。

目前狀態：
- `api-curls/` 是主要 API 操作入口。
- `codex-usage.sh` 讀取本機 `~/.codex/sessions` 的 Codex token_count 事件，可自動輸出目前 usage，並用 `start <label>` / `end <label>` 計算任務前後差值。
- `order.sh`、`transfer.sh`、`snapshot.sh` 是較早期的本機操作腳本。
- `gen-folders.sh` 用於產生或檢查目錄結構。

注意：
- curl 腳本預設打 `http://localhost:8080`。
- `codex-usage.sh` 的快照預設寫到 `/tmp/codex-usage-$USER`，不會弄髒 repo；可用 `CODEX_USAGE_STATE_DIR` 覆蓋。
- 真實私鑰、API key、JWT 不應寫入腳本。
