# AI 上下文路由

## Routing table

```text
Task type                         Required docs
--------------------------------------------------------------------------------
Any task                          AGENTS.md, AI_AGENT.md, AI_PROGRESS.md
Phase 12 schema                   docs/phases/PHASE_12_DATABASE_SCHEMA/README.md
Database migration                docs/phases/PHASE_12_DATABASE_SCHEMA/migration-plan.md
Ledger table design               docs/exchange-core/ledger-invariants.md
Reservation table design          docs/exchange-core/reservation-state-machine.md
Order table design                docs/exchange-core/order-lifecycle.md
Wallet table design               docs/exchange-core/wallet-boundary.md
Backend transaction boundary      docs/backend/transaction-boundary.md
API contract                      docs/backend/api-contract-guidelines.md
Frontend page work                docs/frontend/page-map.md
Operations / deployment           docs/operations/deployment-runbook.md
Readiness review                  docs/governance/PRODUCTION_READINESS_GATES.md
Market Data Pipeline              docs/phases/PHASE_21_MARKET_DATA/README.md, currently relevant task card
Phase 21–36 planning              docs/planning/PHASE_21_36_PLANNING_PROGRAM.md, phase README and relevant draft/charter
```

## Token budget rule

```text
Small task      <= 4 docs
Medium task     <= 8 docs
Architecture    <= relevant directory only
Full repo scan   only with explicit human request
```

## Stop conditions

Stop and ask for human review when the task touches：

```text
money movement
ledger invariants
withdrawal signing
risk bypass
security controls
fee rounding
chain confirmation policy
admin privileged action
```

## Phase 21 路由

```text
phase: Phase 21 - Market Data Pipeline
phase_readme: docs/phases/PHASE_21_MARKET_DATA/README.md
task_card_review: docs/phases/PHASE_21_MARKET_DATA/phase-21-task-card-review.md
proposed_first_task: docs/phases/PHASE_21_MARKET_DATA/p21-t01-inventory-boundary-invariants.md
approval_status: P21-T01 approved and completed, awaiting implementation review; P21-T02 through P21-T08 await explicit human approval
runtime_status: Market Data runtime not started; P21-T01 was documentation-only
completed_task_note: docs/phases/PHASE_21_MARKET_DATA/p21-t01-implementation-review.md
```

## Phase 21–36 規劃路由

```text
planning_program: docs/planning/PHASE_21_36_PLANNING_PROGRAM.md
planning_review: docs/planning/PHASE_21_36_PLANNING_REVIEW.md
phase_22_24: detailed task drafts in each phase README
phase_25_28: mid-level task breakdowns in each phase README
phase_29_36: phase charter and high-level tasks in each phase README
approval_status: all runtime tasks await human review and explicit approval
```
