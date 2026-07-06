# AI Context Routing

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
Readiness review                  docs/PRODUCTION_READINESS_GATES.md
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
