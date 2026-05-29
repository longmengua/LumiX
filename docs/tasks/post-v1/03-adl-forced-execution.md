# Task: ADL Forced Execution

Status: `todo`

## Goal

Move ADL from ranking and planning into forced position/accounting execution with audit trails, operator controls, and deterministic idempotency.

## Scope

- ADL plan consumption and validation.
- Forced opposing position reduction.
- Ledger postings for realized PnL, shortfall, insurance-fund interaction, and fees if applicable.
- Operator halt/manual-review controls.
- Audit events and idempotency keys for repeated execution attempts.

## First Implementation Slice

1. Implement a deterministic service that consumes an ADL plan and emits account, position, ledger, and audit mutations.
2. Guard execution behind risk controls and idempotency checks.
3. Add tests for full execution, repeated request idempotency, insufficient counterparty quantity, and operator halt.
4. Comment the accounting examples so each posting side is readable during review.

## Acceptance Criteria

- ADL can force reduce selected positions and create balanced ledger effects.
- Replaying the same execution command does not double-apply account or position changes.
- Operator halt/manual review prevents execution and leaves an auditable reason.

## Read First

- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
