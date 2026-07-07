# P12 Rollback Notes

Schema rollback in production is risky. Prefer forward fixes after data exists.

## Phase 12 rollback policy

- Never edit applied migrations V001-V008.
- Use a new corrective migration if a schema gap is found after the migration has been shared.
- Keep `V005__create_wallet_lifecycle_schema.sql`, `V006__create_reservation_schema.sql`, `V007__normalize_wallet_lifecycle_schema.sql`, and `V008__create_outbox_audit_idempotency_tables.sql` intact once published.
- If P12-T09 review finds a missing index or constraint, add a new migration instead of rewriting the earlier file.
- P12-T10 is a review gate only; it does not add schema and does not change rollback semantics.

## 在正式資料之前

- Drop and recreate database is acceptable in local/dev only.
- Migration can be rewritten only before shared application.

## 在共享環境上線後

- Do not edit applied migration.
- Add new corrective migration.
- Preserve migration history.
- Document data repair script separately.
