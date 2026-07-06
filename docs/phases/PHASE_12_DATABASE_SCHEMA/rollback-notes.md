# P12 Rollback Notes

Schema rollback in production is risky. Prefer forward fixes after data exists.

## 在正式資料之前

- Drop and recreate database is acceptable in local/dev only.
- Migration can be rewritten only before shared application.

## 在共享環境上線後

- Do not edit applied migration.
- Add new corrective migration.
- Preserve migration history.
- Document data repair script separately.
