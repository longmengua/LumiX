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
Phase 16: in progress as spot trading sandbox scope gate/boundary/runtime foundation, runtime implementation incomplete
P16-T07 completed as sandbox settlement design gate only; settlement runtime not started
P16-T08 completed as sandbox settlement runtime gate only; ledger posting / balance refresh / reservation commit not started
Phase 17-36: planned, not started
Next implementation phase: Phase 16 - Spot Trading Sandbox
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
source_of_truth: docs/OPERATING_EXCHANGE_MASTER_PLAN.md
agent_rules: AGENTS.md and AI_AGENT.md
context_router: docs/ai/AI_CONTEXT_ROUTING.md
phase_governance: docs/PHASE_REVIEW_WORKFLOW.md
next_implementation_phase: docs/phases/PHASE_15_BALANCE_RECON/README.md
first_task: docs/phases/PHASE_15_BALANCE_RECON/README.md
```

## 完成警告

在就緒門檻全部通過前，不要聲稱正式交易已完成。
Do not claim production launch ready before 第 36 階段 and explicit human sign-off.
