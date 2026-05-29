# Task: ADL Forced Execution

Status: `doing`

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
- Added focused tests for full execution, repeated command id idempotency, durable-store idempotency across service instances, insufficient candidate quantity pre-mutation rejection, and operator halt audit.

Remaining work:
- Add command transaction boundary coverage and Redis hot-state repair rules after DB commit.
- Add API/operator ownership workflow and wire ADL queue entries into plan/execution orchestration.
- Harden insurance-fund interaction and retry semantics around partially covered liquidation shortfalls.

## Acceptance Criteria

- ADL can force reduce selected positions and create balanced ledger effects.
- Replaying the same execution command does not double-apply account or position changes.
- Operator halt/manual review prevents execution and leaves an auditable reason.

## Read First

- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
