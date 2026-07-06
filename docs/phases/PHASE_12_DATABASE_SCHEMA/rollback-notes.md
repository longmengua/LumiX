# P12 Rollback Notes

Schema rollback in production is risky. Prefer forward fixes after data exists.

## Before production data

- Drop and recreate database is acceptable in local/dev only.
- Migration can be rewritten only before shared application.

## After shared application

- Do not edit applied migration.
- Add new corrective migration.
- Preserve migration history.
- Document data repair script separately.
