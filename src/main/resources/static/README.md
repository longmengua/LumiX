# src/main/resources/static

靜態資源。

目前內容：
- `index.html`：本機真實交易測試控制台。
- `admin-market-config.html`：read-only admin market-config 檢視頁，搭配 `/api/admin/market-config`。
- `admin-risk-parameters.html`：read-only admin risk parameter / switch 檢視頁，搭配 `/api/admin/risk-parameters`。
- `admin-dlq.html`：read-only admin DLQ 檢視頁，搭配 `/api/admin/dlq`，payload / headers 只顯示 sanitizied preview。

注意：
- 這不是 production 前端。
- 若新增正式前端，應先確認 build pipeline、auth flow、secret handling。
