# 第 12 階段 - 生產資料庫結構與遷移

## 狀態

```text
in progress
```

## 目標

建立後續階段可以安全依賴的正式資料庫基礎。

## 不在範圍內

```text
runtime ledger mutation
runtime balance mutation
matching execution
settlement execution
deposit crediting
withdrawal signing / broadcast
```

## 必要閱讀

```text
AGENTS.md
AI_AGENT.md
docs/ai/AI_CONTEXT_ROUTING.md
docs/backend/transaction-boundary.md
docs/exchange-core/ledger-invariants.md
docs/exchange-core/reservation-state-machine.md
```

## 任務順序

```text
P12-T01 migration tool and directory conventions
P12-T02 identity, user, account, and asset tables
P12-T03 balance projection tables
P12-T04 ledger journal and entries
P12-T05 order, trade, reservation, settlement schema
P12-T06 deposit, withdrawal, address, chain transaction schema
P12-T07 outbox, audit log, idempotency, admin action schema
P12-T08 constraints, indexes, uniqueness, precision rules
P12-T09 schema verification tests and rollback notes
```

## 完成條件

- Migrations exist.
- Schema can be applied to a clean PostgreSQL database.
- Core constraints and indexes are defined.
- Schema verification exists.
- 要有回滾／修復策略文件。
- 這個階段不會實作 runtime 資金異動。
