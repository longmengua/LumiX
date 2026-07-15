# Operating Exchange Master Plan

本文件是 LumiX 從文件架構進入正式營運交易所的總綱。

## Current phase

```text
Phase 11: 生產架構重整 - 以文件完成
Phase 12: 生產資料庫結構與 migration - completed
Phase 13: backend module foundation & API boundary - completed
Phase 14: Immutable Ledger Engine foundation - completed, not production-ready
Phase 15: COMPLETED_FOR_TRADING_RUNTIME_CORE_FOUNDATION
  - Phase 15 backend foundation gates completed
  - Phase 15 trading runtime core foundation completed
  - NOT production-ready
  - NOT full trading runtime
  - NOT order/matching/settlement ready
  - NOT reservation runtime ready
  - NOT settlement runtime ready
  - NOT futures/liquidation/withdrawal ready
  - NOT exchange ready
  - NOT public user trading ready
Phase 16: COMPLETED_FOR_SPOT_SANDBOX_FOUNDATION
  - Spot sandbox flow foundation completed
  - NOT production-ready
  - NOT public user trading ready
  - NOT real-money ready
  - NOT ledger-posting-integrated
  - NOT balance-updating
  - NOT reservation-backed
  - NOT settlement-finalized
  - NOT withdrawal-ready
  - NOT futures/margin/liquidation ready
  - P16-T10 completed as final review gate only; spot sandbox flow foundation已收斂，但 production runtime 仍未開始
Phase 17: COMPLETED
  - Phase 17 human review: APPROVED
  - Phase 17 人工審核完成
  - Futures core sandbox model foundation implemented
  - NOT production-ready
  - NOT public futures trading ready
  - NOT real-money ready
  - NOT order-intake-ready
  - NOT matching-ready
  - NOT settlement-ready
  - NOT ledger-integrated
  - NOT balance-reservation-backed
  - NOT liquidation-ready
  - NOT funding-ready
  - NOT full margin-engine-ready
Phase 18: NEXT PLANNED PHASE — NOT STARTED
Phase 19-36: planned, not started
```

## Master phase ladder

```text
P12 Database Schema & Migration
  |
P13 Identity, Account, Asset Foundation
  |
P14 Immutable Ledger Engine
  |
P15 Trading Runtime Core
  |
P16 Spot Trading Sandbox
  |
P17 Futures Core Model
  |
P18 Futures Trading Sandbox
  |
P19 Risk Sandbox
  |
P20 Contract Trading Integration Gate
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
P30 前端正式交易 UX
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
P36 正式上線門檻
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

## Accelerated track rules

```text
P15 到 P20 是加速但安全的交易 sandbox 路線，不是 production-ready。
P18 最早只能做受限 futures sandbox，不能視為正式合約交易。
P20 才是較完整 contract trading sandbox gate，但仍然不是正式交易上線門檻。
production-ready 仍然需要後續 security、ops、risk、liquidity、monitoring、incident response review。
所有 ledger / balance / reservation / settlement / futures / liquidation / withdrawal / admin / security runtime 都屬於 HUMAN_REVIEW_REQUIRED。
Phase 16 已完成 sandbox foundation flow；仍不能誤寫成正式 matching、settlement、ledger posting 或 production runtime。
```
