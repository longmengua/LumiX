# Task: Trade Surveillance SAR

Status: `todo`

## Goal

Add a trade-surveillance baseline for suspicious activity detection, review queues, and suspicious-activity report preparation.

## Scope

- Rule inventory for wash trading, self-trading, rapid cancel/replace, unusual volume, and market manipulation indicators.
- Detection service or batch job with disabled-by-default rule execution.
- Review queue model with case status, owner, severity, notes, and related trades/orders/accounts.
- Suspicious-activity report draft/export fields without external regulatory filing integration.

## First Implementation Slice

1. Define surveillance signal DTOs and rule result categories.
2. Add one or two deterministic rules using existing trade/order projections.
3. Add tests for positive, negative, and duplicate-signal cases.
4. Add operator review task/API only after security classification is clear.
5. Document limitations and false-positive review expectations.

## Acceptance Criteria

- Detection results are reproducible for a fixed trade/order dataset.
- Cases include source evidence and do not expose unnecessary PII.
- SAR draft/export fields are prepared but not automatically filed.

## Read First

- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)

