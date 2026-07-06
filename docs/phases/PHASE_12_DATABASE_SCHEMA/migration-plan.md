# P12 Migration Plan

## Suggested order

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

## Migration rules

- Use deterministic names.
- Do not edit an already-applied migration in shared environments.
- Add new migration for changes.
- Include comments for high-risk tables.
- Keep rollback notes in `rollback-notes.md`.
