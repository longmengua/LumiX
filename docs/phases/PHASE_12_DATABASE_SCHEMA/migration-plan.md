# P12 遷移計畫

## 建議順序

```text
V001__create_identity_and_assets.sql
V002__create_balance_projection_tables.sql
V003__create_ledger_tables.sql
V004__create_order_and_trade_schema.sql
V005__create_wallet_lifecycle_schema.sql
V006__create_reservation_schema.sql
V007__create_outbox_audit_idempotency_tables.sql
V008__add_indexes_and_constraints.sql
V009__add_schema_comments.sql
```

## 遷移規則

- 請使用可決定性的名稱。
- 不要在共享環境中編輯已套用的 migration。
- 有變更就新增 migration。
- 高風險資料表要加上註解。
- 回滾備註請保留在 `rollback-notes.md`。
- `V005__create_wallet_lifecycle_schema.sql` 已先行建立，必須保留，不可 revert。
- 真正的 reservation / hold / release schema 由 `V006__create_reservation_schema.sql` 補上；P12-T06 不能視為 reservation 完成。
