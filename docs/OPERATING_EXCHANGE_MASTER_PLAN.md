# Operating Exchange Master Plan

本文件是 LumiX 從文件架構進入正式營運交易所的總綱。

## Current phase

```text
Phase 11: Production architecture reset - completed as docs-only
Phase 12: Production database schema and migration - next
Phase 13-36: planned, not started
```

## Master phase ladder

```text
P12 Database Schema & Migration
  |
P13 Identity, Account, Asset Foundation
  |
P14 Immutable Ledger Engine
  |
P15 Balance Projection & Reconciliation
  |
P16 Reservation / Freeze Engine
  |
P17 Order Intake & Idempotency
  |
P18 Matching Core Contract
  |
P19 Trade Settlement Engine
  |
P20 Fee Engine & Revenue Accounting
  |
P21 Market Data Pipeline
  |
P22 Deposit Address & Chain Listener
  |
P23 Deposit Crediting & Confirmation Policy
  |
P24 Withdrawal Request Workflow
  |
P25 Withdrawal Approval / Signing / Broadcast
  |
P26 Risk Control & Limits
  |
P27 Admin Console Foundation
  |
P28 Audit, Compliance, Evidence Export
  |
P29 Public / Private API Hardening
  |
P30 Frontend Production Trading UX
  |
P31 Observability & Alerting
  |
P32 Disaster Recovery & Replay
  |
P33 Security Hardening
  |
P34 Load / Soak / Chaos Testing
  |
P35 Business Operations Readiness
  |
P36 Production Launch Gate
```

## No-jump rule

```text
A later phase may read earlier design.
A later phase may not be implemented before its dependency phases pass review.
```

## Business readiness view

```text
Revenue requires:
  - fee policy
  - fee calculation
  - fee ledger posting
  - reporting
  - reconciliation
  - admin visibility
  - tax / accounting export path
```

不應只做到交易畫面與 order API 就認定能營運。能賺錢代表費用、收入、風險、事故與對帳都能落地。
