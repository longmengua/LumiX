# Task: Liquidation And ADL

Status: `todo`

## Goal

Complete production-grade liquidation and ADL behavior beyond the current MVP: scanning, execution routing, ADL queue ranking, insurance-fund interaction, audit events, and operator controls.

## Scope

- Liquidation scanning and trigger evaluation.
- Forced close execution policy.
- ADL queue ranking by risk/profit/leverage criteria.
- Insurance fund debit/credit and shortfall behavior.
- Operator controls for halt, manual review, retry, and audit.

## First Implementation Slice

1. Review current `LiquidationService`, `InsuranceFundService`, and risk tests.
2. Add deterministic ADL ranking model and tests.
3. Add audit event coverage for liquidation/ADL decisions.
4. Add operator-control hooks before routing execution.

## Acceptance Criteria

- ADL queue ranking is deterministic and covered by tests.
- Liquidation execution path records auditable decision data.
- Insurance fund and shortfall accounting remain reconcilable.

## Read First

- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
