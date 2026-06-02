# Task: Fee Report Service

Status: `todo`

## Goal

Add a backend report service for fee charges, rebates, funding-related fees, and fee ledger references.

## Scope

- Query fees by uid, symbol, fee type, source, and accounting window.
- Include fee amount, currency/asset, related trade/order ids, ledger entry refs, and calculation basis where available.
- Separate actual charged fees from estimated or pending fee fields.
- Provide export-ready rows for finance operations.

## First Implementation Slice

1. Map fee-related ledger postings and trade/order references.
2. Add service DTOs and query method for bounded fee windows.
3. Add tests for maker/taker-style fees, funding/liquidation fee categories if present, and empty-result behavior.
4. Document unavailable fee basis fields.

## Acceptance Criteria

- Report rows tie charged fees back to immutable ledger refs.
- Pending/estimated fees are not mixed with posted fees.
- Tests cover filtering by uid and fee type.

## Read First

- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)

