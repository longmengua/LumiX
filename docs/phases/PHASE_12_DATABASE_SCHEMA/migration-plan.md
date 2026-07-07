# P12 遷移計畫

## 建議順序

```text
V001__create_identity_and_assets.sql
V002__create_balance_projection_tables.sql
V003__create_ledger_tables.sql
V004__create_order_and_trade_schema.sql
V005__create_wallet_lifecycle_schema.sql
V006__create_reservation_schema.sql
V007__normalize_wallet_lifecycle_schema.sql
V008__create_outbox_audit_idempotency_tables.sql
V009__add_indexes_and_constraints.sql
V010__add_schema_comments.sql
```

## 遷移規則

- 請使用可決定性的名稱。
- 不要在共享環境中編輯已套用的 migration。
- 有變更就新增 migration。
- 高風險資料表要加上註解。
- 回滾備註請保留在 `rollback-notes.md`。
- `V005__create_wallet_lifecycle_schema.sql` 已先行建立，必須保留，不可 revert。
- `V006__create_reservation_schema.sql` 是 P12-T06 的正式實作點；reservation schema 由這份 migration 完成，wallet lifecycle schema 仍保留在 V005。
- `V007__normalize_wallet_lifecycle_schema.sql` 只補 wallet lifecycle 的查詢索引，不改 V005 欄位語意。
- `V008__create_outbox_audit_idempotency_tables.sql` 是 P12-T08 的正式實作點；以 TEXT 保存 payload 與 state snapshot，維持測試相容性。
- P12-T09 只做驗證與 rollback 文件收斂，不新增 business domain tables；若之後發現缺索引 / constraint，請用新的 corrective migration，不要回寫 V001-V008。
- P12-T10 只做 final review gate 與狀態收斂，不新增 migration；若後續 review 仍有缺口，仍應以新 corrective migration 或文件補強處理，不可回寫 V001-V008。
