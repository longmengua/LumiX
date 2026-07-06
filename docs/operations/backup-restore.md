# 備份與還原

## Backup scope

```text
PostgreSQL
configuration
migration history
audit logs
object storage if used
```

## Restore validation

```text
restore database
run migrations check
run ledger invariant check
rebuild balance projection
compare sampled balances
verify API read-only smoke tests
```
