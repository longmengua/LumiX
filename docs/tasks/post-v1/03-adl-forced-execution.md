# Task: ADL Forced Execution

Status: `done`

## Goal

Move ADL from ranking and planning into forced position/accounting execution with audit trails, operator controls, and deterministic idempotency.

## Scope

- ADL plan consumption and validation.
- Forced opposing position reduction.
- Ledger postings for realized PnL, shortfall, insurance-fund interaction, and fees if applicable.
- Operator halt/manual-review controls.
- Audit events and idempotency keys for repeated execution attempts.

## First Implementation Slice

1. [x] Implement a deterministic service that consumes an ADL plan and emits account, position, ledger, and audit mutations.
2. [x] Guard execution behind risk controls and idempotency checks.
3. [x] Add tests for full execution, repeated request idempotency, insufficient counterparty quantity, and operator halt.
4. [x] Comment the accounting examples so each posting side is readable during review.

## Progress

- Added `AdlForcedExecutionService` to validate an `AdlDeleveragingPlan` before mutation, then force reduce selected positions at the plan-implied execution price.
- Added `AdlExecutionResult` and `AdlExecutionStepResult` for deterministic execution summaries.
- Added `AdlForcedDeleveragingRecorded` audit events for executed, halted, and manual-review decisions.
- Added `WalletLedgerService.applyAdlForcedLoss(...)` so profitable ADL counterparties can have realized profit credited first and then charged through a balanced `adl_forced_loss` posting.
- Added `AdlExecutionStore`, `AdlExecutionRecordEntity`, `JpaAdlExecutionStore`, and Flyway migration `V2__adl_execution_records.sql` for durable execution summary/idempotency records.
- The service still falls back to a service-local command id cache when no durable store is configured, but Spring runtime can now use the JPA store.
- Added `ExecuteAdlCommand` and `ExecuteAdlUseCase` so ADL forced execution can enter `CommandTransactionBoundary` in Spring runtime.
- Added `AdlQueueExecutionService` to consume an ADL queue entry, filter opposite-side profitable candidates, rank/plan the forced reduction, execute through `ExecuteAdlUseCase`, and complete the queue entry after full coverage.
- Added `POST /api/risk/adl-queue/{liquidationId}/claim`, `/execute`, and `/release` plus curl examples for operator-triggered queue ownership/execution with command idempotency.
- Added `GET /api/risk/adl-queue/alerts?minAgeSeconds=...` and curl coverage for aged open/stuck claimed ADL queue alert reports.
- Added durable insurance-fund movement records, `V21__insurance_fund_movements.sql`, JPA/in-memory stores, `GET /api/risk/insurance-fund/movements`, and curl/test coverage.
- Claimed queue entries now reject execution attempts from a different operator id.
- Documented ADL hot-state repair rules for cases where DB execution commits but Redis/in-memory queue or account/position projections drift.
- Partial ADL execution now updates the queue item to the remaining notional instead of retrying the original amount.
- Added focused tests for full execution, repeated command id idempotency, durable-store idempotency across service instances, insufficient candidate quantity pre-mutation rejection, and operator halt audit.

Follow-up hardening:
- Add stronger operator assignment audit history.

## Acceptance Criteria

- ADL can force reduce selected positions and create balanced ledger effects.
- Replaying the same execution command does not double-apply account or position changes.
- Operator halt/manual review prevents execution and leaves an auditable reason.

## Read First

- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
