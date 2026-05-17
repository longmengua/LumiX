# shells

這裡放本機操作腳本與 API curl 範例。

目前狀態：
- `api-curls/` 是主要 API 操作入口。
- `order.sh`、`transfer.sh`、`snapshot.sh` 是較早期的本機操作腳本。
- `gen-folders.sh` 用於產生或檢查目錄結構。

注意：
- curl 腳本預設打 `http://localhost:8080`。
- 真實私鑰、API key、JWT 不應寫入腳本。
