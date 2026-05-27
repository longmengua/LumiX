# src/main/resources/db/migration

Flyway migration scripts。

目前內容：
- `V1__reliability_baseline.sql`：reliability baseline schema。
- `V2__order_lifecycle_projection.sql`：order lifecycle event log 與最新狀態 projection。
- `V3__wallet_ledger_journal.sql`：durable double-entry wallet ledger journal。
- `V4__reconciliation_reports.sql`：persisted reconciliation report 與 issue 明細。
- `V5__durable_outbox_headers.sql`：補齊 durable outbox headers 與 manual compensation 相容欄位。
- `V6__account_risk_snapshots.sql`：account risk snapshot 持久化表。
- `V7__matching_replay_logs.sql`：matching command/event log、offset checkpoint、engine snapshot 與 replay validation report 持久化 baseline。

注意：
- migration 檔案不可修改已發布版本；新增變更應建立下一個 `V{n}__*.sql`。
- Flyway 是 schema 唯一管理入口；Hibernate 只做 `validate`，不得用 `ddl-auto=update` 漂移 schema。
- production index、ledger schema、event projection schema 都應在這裡落地。
