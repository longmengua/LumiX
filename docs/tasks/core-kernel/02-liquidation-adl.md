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

Remaining work:
- Wire ADL deleveraging plans into actual position/accounting execution.
- Add retry/ownership workflow for operator-reviewed liquidation decisions.

## Acceptance Criteria

- ADL queue ranking is deterministic and covered by tests.
- Liquidation execution path records auditable decision data.
- Insurance fund and shortfall accounting remain reconcilable.

## Read First

- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
