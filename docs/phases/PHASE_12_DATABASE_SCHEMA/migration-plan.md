# P12 遷移計畫

## 建議順序

```text
V001__create_identity_and_assets.sql
V002__create_ledger_tables.sql
V003__create_balance_projection_tables.sql
V004__create_reservation_and_order_tables.sql
V005__create_wallet_tables.sql
V006__create_outbox_audit_idempotency_tables.sql
V007__add_indexes_and_constraints.sql
V008__add_schema_comments.sql
```

## 遷移規則

- 請使用可決定性的名稱。
- 不要在共享環境中編輯已套用的 migration。
- 有變更就新增 migration。
- 高風險資料表要加上註解。
- 回滾備註請保留在 `rollback-notes.md`。
