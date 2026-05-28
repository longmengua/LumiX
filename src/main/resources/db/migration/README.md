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
- `V8__matching_sequencer_leases.sql`：matching sequencer lease / epoch fencing baseline。
- `V9__matching_cancel_replace_commands.sql`：matching cancel-replace command replacement order payload。
- `V10__matching_owner_epoch_logs.sql`：matching command/event log owner epoch 審計欄位。
- `V11__turnover_records.sql`：成交流水 read model，支援 user、symbol、strategy、market-maker、order、match 與 sequence 維度。
- `V12__bonus_credit_grants.sql`：體驗金 grant 批次 read model，支援 remaining amount、status 與 expiry 掃描。
- `V13__reconciliation_issue_workflow.sql`：reconciliation issue workflow 欄位，支援 status、owner、resolved_at。
- `V14__market_maker_profiles.sql`：做市商 profile 與 per-symbol risk limit 持久化。
- `V15__hedge_decision_audits.sql`：做市商 hedge decision audit trail 持久化。
- `V16__hedge_fills.sql`：做市商 hedge fill audit trail 持久化。

注意：
- migration 檔案不可修改已發布版本；新增變更應建立下一個 `V{n}__*.sql`。
- Flyway 是 schema 唯一管理入口；Hibernate 只做 `validate`，不得用 `ddl-auto=update` 漂移 schema。
- production index、ledger schema、event projection schema 都應在這裡落地。
