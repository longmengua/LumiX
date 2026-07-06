# P12 測試

## 必要檢查

```text
clean database migration applies
repeat migration state is stable
money columns avoid float/double
ledger table append-only intent documented
foreign keys exist for critical references
unique constraints exist for idempotency keys
indexes exist for query paths
```

## 建議測試類型

- Migration 整合測試。
- Schema 中繼資料斷言。
- 如果有 repository 類別，就做 smoke test。
- 對 migration SQL 做靜態 grep，檢查禁止的金額型別。
