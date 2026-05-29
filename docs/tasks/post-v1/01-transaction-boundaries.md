# Task: Transaction Boundaries

Status: `doing`

## Goal

Define and enforce explicit write boundaries for MySQL, Redis, Kafka, matching state, ledger postings, and outbox publishing so core writes never assume cross-store atomicity.

## Scope

- Order placement and cancellation write sequence.
- Trade processing, account updates, position updates, and ledger postings.
- Outbox event writes and publish retry behavior.
- Redis hot-state updates and recovery behavior when database writes succeed but cache/event publication fails.
- Idempotency and retry semantics for command handlers.

## First Implementation Slice

1. [x] Document the current write sequence for one order-to-trade path.
2. [x] Add a command-level transaction boundary service for that path.
3. [x] Ensure database state and outbox rows are written inside the same transaction.
4. [x] Add tests that explain rollback, retry, and duplicate-command behavior with comments around each business invariant.

## Progress

- Added `CommandTransactionBoundary` as the shared command-level transaction wrapper for core writes.
- Wired `PlaceOrderUseCase`, `CancelOrderUseCase`, `AmendOrderUseCase`, and `CancelReplaceOrderUseCase` through the boundary when Spring provides it; direct unit tests can still instantiate the use cases without Spring transaction infrastructure.
- `CancelReplaceOrderUseCase` now owns an outer boundary so cancel original and place replacement share one database transaction in Spring runtime.
- Wired `LiquidateUseCase` through the same boundary so manual liquidation enters a command transaction before position, ledger, insurance/ADL queue, and audit event work.
- Wired `MarketMakerHedgeExecutionService` manual and enabled-profile execution through the same boundary so profile lookup, exposure planning, hedge routing, and audit publish share one command entry.
- Updated `OutboxService` so `publish(...)` saves the outbox row immediately but defers the external publisher call until Spring transaction `afterCommit` when a transaction is active.
- Added tests proving successful command bodies commit, failed command bodies roll back without hidden retry, active transactions do not send outbox payloads before commit, and liquidation/hedge execution enter the boundary when configured.

Remaining work:
- Apply the same boundary to ADL forced execution when that service exists.
- Add persistence-backed integration tests that prove DB state and outbox rows roll back together under MySQL.
- [x] Document Redis hot-state recovery rules for cases where DB commits but cache writes fail.

## Acceptance Criteria

- Core write paths declare which state is authoritative after partial failure.
- Tests cover success, rollback, duplicate command, and outbox retry behavior.
- AI maps are updated with the final transaction boundary and recovery assumptions.

## Read First

- [../../ai/maps/reliability-market-data.md](../../ai/maps/reliability-market-data.md)
- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
