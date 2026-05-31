# Task: Liquidation And ADL

Status: `doing`

## Goal

Complete production-grade liquidation and ADL behavior beyond the current MVP: scanning, execution routing, ADL queue ranking, insurance-fund interaction, audit events, and operator controls.

## Scope

- Liquidation scanning and trigger evaluation.
- Forced close execution policy.
- ADL queue ranking by risk/profit/leverage criteria.
- Insurance fund debit/credit and shortfall behavior.
- Operator controls for halt, manual review, retry, and audit.

## First Implementation Slice

1. [x] Review current `LiquidationService`, `InsuranceFundService`, and risk tests.
2. [x] Add deterministic ADL ranking model and tests.
3. [x] Add audit event coverage for liquidation/ADL decisions.
4. [x] Add operator-control hooks before routing execution.

## Progress

- Current `LiquidationService` closes under-margined positions, releases position margin, applies realized PnL, covers shortfall through insurance fund, and enqueues ADL residual shortfall.
- Added `AdlRankingCandidate`, `AdlRankedPosition`, and `AdlRankingService`.
- ADL ranking is deterministic: higher profit rate, higher effective leverage, larger notional, then lower uid.
- Added `AdlRankingServiceTest` for ranking order and exclusion of loss/zero/invalid candidates.
- Added `LiquidationDecisionRecorded` audit event and test coverage for liquidation decision reason and insurance/ADL coverage.
- Added `risk-controls.liquidation-halt` and `risk-controls.liquidation-manual-review` operator hooks.
- Manual review mode records a decision audit event without closing the position or writing liquidation ledger entries.
- Added `LiquidationScanService` to scan open positions and trigger oracle-based liquidation decisions.
- Added `AdlDeleveragingPlanner` to turn ranked ADL candidates into deterministic forced-deleveraging steps.
- Added `AdlForcedExecutionService` first slice to consume ADL plans, force reduce selected positions, write realized-PnL and `adl_forced_loss` ledger postings, publish audit events, and persist durable execution summary/idempotency records.
- Added `AdlQueueExecutionService` to consume queued liquidation shortfalls, filter opposite-side ADL candidates, plan reduction, execute through `ExecuteAdlUseCase`, enforce claimed-entry owner guard, and keep queue entries retryable when only partial coverage or no eligible candidates are available.
- ADL queue enqueue is idempotent by `liquidationId`; duplicate liquidation retry/replay does not create another queue entry or clear an existing operator claim.
- `ExecuteAdlUseCase` now enters `CommandTransactionBoundary`, so ADL queue-to-execution routes through the same command transaction baseline as other core writes.
- Added `AdlQueueStore` with in-memory and JPA adapters plus `adl_queue_entries` Flyway schema so queue state and operator claims survive process restarts.
- Added `GET /api/risk/adl-queue/stuck-claims?minClaimAgeSeconds=...` to report claimed ADL entries that have exceeded the operator age threshold.
- Added `GET /api/risk/adl-executions?limit=...` for recent forced-deleveraging outcomes.
- Added `GET /api/risk/adl-insurance-reconciliation?asset=...` to compare open ADL queue shortfalls with liquidated-position ADL/insurance coverage.
- Added ADL operator runbooks for stuck claims, partial retries, and no-candidate retries.
- Added restart-style partial retry coverage showing a restarted executor consumes only persisted remaining queue notional.

Remaining work:
- Add retry/ownership workflow for operator-reviewed liquidation decisions.
- Add alert-backend delivery for stuck claims, production insurance-fund capital movement records, and stronger operator assignment audit history.

## Acceptance Criteria

- ADL queue ranking is deterministic and covered by tests.
- Liquidation execution path records auditable decision data.
- Insurance fund and shortfall accounting remain reconcilable.

## Read First

- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
