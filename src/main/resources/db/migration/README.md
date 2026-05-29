# src/main/resources/db/migration

Flyway migration scripts。

目前內容：
- `V1__core_v1_baseline.sql`：core-v1 乾淨 baseline schema，合併原 V1-V20 的可靠性、order lifecycle、ledger、reconciliation、matching replay/lease、turnover、bonus credit、market-maker、hedging、prediction / Polymarket tables。

注意：
- 目前尚未正式發布 production schema；Docker volume 清空後可用單一 baseline 重新開始。
- core-v1 之後若已對外發布 migration，後續變更應建立下一個 `V{n}__*.sql`，不要再修改既有版本。
- Flyway 是 schema 唯一管理入口；Hibernate 只做 `validate`，不得用 `ddl-auto=update` 漂移 schema。
- production index、ledger schema、event projection schema 都應在這裡落地。
