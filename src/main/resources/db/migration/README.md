# src/main/resources/db/migration

Flyway migration scripts。

目前內容：
- `V1__reliability_baseline.sql`：reliability baseline schema。

注意：
- migration 檔案不可修改已發布版本；新增變更應建立下一個 `V{n}__*.sql`。
- production index、ledger schema、event projection schema 都應在這裡落地。
