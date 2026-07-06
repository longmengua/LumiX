# P12 Tests

## Required checks

```text
clean database migration applies
repeat migration state is stable
money columns avoid float/double
ledger table append-only intent documented
foreign keys exist for critical references
unique constraints exist for idempotency keys
indexes exist for query paths
```

## Suggested test types

- Migration integration test.
- Schema metadata assertion.
- Repository smoke test if repository classes exist.
- Static grep for forbidden money types in migration SQL.
