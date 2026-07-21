# AI_PROGRESS.md

## Authoritative status

```text
Phase 11: completed as documentation-only production architecture reset
Phase 12: completed as production database schema and migration foundation
Phase 13: completed as backend module foundation and API boundary
Phase 14: completed as immutable ledger engine foundation, append-only adapter verified on PostgreSQL, not production-ready
Phase 15: COMPLETED_FOR_TRADING_RUNTIME_CORE_FOUNDATION
Phase 15 backend foundation gates completed
Phase 15 trading runtime core foundation completed
NOT production-ready
NOT full trading runtime
NOT order/matching/settlement ready
NOT reservation runtime ready
NOT settlement runtime ready
NOT futures/liquidation/withdrawal ready
NOT exchange ready
NOT public user trading ready
Phase 16: COMPLETED_FOR_SPOT_SANDBOX_FOUNDATION
Spot sandbox flow foundation completed
NOT production-ready
NOT public user trading ready
NOT real-money ready
NOT ledger-posting-integrated
NOT balance-updating
NOT reservation-backed
NOT settlement-finalized
NOT withdrawal-ready
NOT futures/margin/liquidation ready
P16-T10 completed as final review gate only; spot sandbox flow foundation已收斂，但 production runtime 仍未開始
P16-T07 completed as sandbox settlement design gate only; settlement runtime not started
P16-T08 completed as sandbox settlement runtime gate only; ledger posting / balance refresh / reservation commit not started
P16-T09 completed as sandbox settlement / ledger integration design gate only; ledger posting runtime not started
Phase 17: COMPLETED
Phase 17 human review: APPROVED
Phase 17 人工審核完成
Futures core sandbox model foundation implemented
NOT production-ready
NOT public futures trading ready
NOT real-money ready
NOT order-intake-ready
NOT matching-ready
NOT settlement-ready
NOT ledger-integrated
NOT balance-reservation-backed
NOT liquidation-ready
NOT funding-ready
NOT full margin-engine-ready
Phase 18: COMPLETED_FOR_FUTURES_TRADING_SANDBOX_FOUNDATION
Phase 18 human review: APPROVED
Restricted futures trading sandbox foundation implemented
NOT production-ready
NOT public futures trading ready
NOT real-money ready
NOT matching-execution-ready
NOT fill-producer-ready
NOT balance-reservation-backed
NOT ledger-integrated
NOT settlement-ready
NOT liquidation-ready
NOT formal funding-engine-ready
Phase 19: COMPLETED_FOR_RISK_SANDBOX_FOUNDATION
Phase 19 human review: APPROVED
Risk sandbox foundation implemented
NOT production liquidation ready
NOT formal funding engine ready
NOT insurance fund accounting ready
NOT ledger/balance reconciliation runtime ready
Phase 20: COMPLETED_FOR_CONTRACT_TRADING_INTEGRATION_GATE_FOUNDATION
Phase 20 human review: APPROVED
Contract Trading Integration Gate sandbox foundation implemented
NOT production-ready
NOT formal contract trading launched
NOT public contract trading ready
NOT real-money contract trading ready
NOT matching or fill execution enabled
NOT position, balance or ledger updated
NOT settlement completed
Phase 21: PLANNED_NOT_STARTED
Phase 21 Market Data Pipeline awaits explicit human kickoff and approved task cards
Phase 22-36: planned, not started
Next implementation phase: Phase 21; do not implement before explicit human kickoff and task-card review
```

## 目前倉庫現況

```text
web/    : React + TypeScript + Vite frontend foundation and mock/development adapters
server/ : Java 21 + Spring Boot 3 foundation, interfaces, DTOs, and stubs
docs/   : production architecture and phase planning documents
```

不要把正式帳本引擎、凍結引擎、撮合核心、結算引擎、真實入金系統、真實提款系統或正式行情資料管線視為已完成。

## 目前任務指引

```text
source_of_truth: docs/governance/OPERATING_EXCHANGE_MASTER_PLAN.md
agent_rules: AGENTS.md and AI_AGENT.md
context_router: docs/ai/AI_CONTEXT_ROUTING.md
phase_governance: docs/governance/PHASE_REVIEW_WORKFLOW.md
phase_20_review_status: docs/phases/PHASE_20_FEE_ENGINE/phase-20-final-review.md
current_phase_task_list: docs/phases/PHASE_21_MARKET_DATA/README.md
next_implementation_phase: Phase 21; awaits explicit human kickoff and approved task cards
first_task: none; define and review the first Phase 21 task card before implementation
```

## 完成警告

在就緒門檻全部通過前，不要聲稱正式交易已完成。
Do not claim production launch ready before 第 36 階段 and explicit human sign-off.
