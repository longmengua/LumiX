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
approval_status: awaiting human review; P21-T01 is not approved for implementation
runtime_status: not started
```
