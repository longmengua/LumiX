# Phase 14 - PostgreSQL Append Adapter Verification

## 目的

這份文件只記錄 P14-T06 append adapter 在 PostgreSQL 16 上的 replay 與驗證結果。
它不是正式 posting runtime 文件，也不是 migration 文件。

## 驗證範圍

```text
PostgreSQL 16
V001 到 V008 migration replay
seed users / accounts / assets / account_assets
valid ledger mapping 可 append
entry insert 失敗時 rollback
balance_projections row count 不變
不出現 update / delete ledger_journals 或 ledger_entries
posting command boundary 不接 DB adapter
```

## 驗證策略

```text
優先以最小手動 PostgreSQL replay 驗證
不新增正式 runtime
不新增 @Transactional
不新增 @Repository
不接 LedgerPostingCommandBoundary
若未使用 Testcontainers，CI 仍需另外準備 PostgreSQL 環境才能重放
```

## 驗證結果

```text
PostgreSQL version: 16.14
Replayed migrations: V001, V002, V003, V004, V005, V006, V007, V008
Append verification result: PASS
Rollback verification result: PASS
balance_projections row count: 0
Posting command boundary connected to DB adapter: no
@Repository / @Transactional: no
Testcontainers: no
```

## 驗證命令

```text
cd server
./mvnw -Dtest=ModuleDependencyGuardrailTest,LedgerAppendOnlyJdbcAdapterTest,LedgerAppendOnlyPostgresVerificationTest,P14T06LedgerAppendPersistenceAdapterTest,LedgerAppendTransactionPolicyTest,P14T05LedgerAppendTransactionBoundaryTest,LedgerPostingCommandBoundaryTest,P14T04LedgerPostingBoundaryTest,LedgerAppendOnlyPersistenceMappingTest,P14T03LedgerPersistenceBoundaryTest,LedgerInvariantPolicyTest,P14T02LedgerDomainBoundaryTest test -Dlumix.postgres.jdbc-url=jdbc:postgresql://localhost:55432/lumix_p14_t07 -Dlumix.postgres.username=postgres -Dlumix.postgres.password=rootpass
```

## HUMAN_REVIEW_REQUIRED

```text
任何把這份驗證文件誤解成 production readiness 證明的行為都屬於 HUMAN_REVIEW_REQUIRED。
任何把 PostgreSQL verification 擴張成正式 posting runtime 的變更都屬於 HUMAN_REVIEW_REQUIRED。
```
