# Task: Trade Report Service

Status: `todo`

## Goal

Add a backend report service for user, symbol, and time-window trade reports across core matching and supported external venues.

## Scope

- Query trade rows by uid, symbol, venue/source, side, and time range.
- Include order id, client order id, trade id, price, quantity, fee refs, ledger refs, and event/source timestamps.
- Reconcile report fields with durable trade tape and order lifecycle projections.
- Support bounded pagination and export cursors.

## First Implementation Slice

1. Map durable trade tape and order lifecycle projection fields.
2. Add report DTOs and service method for bounded trade queries.
3. Add tests for pagination, uid filtering, symbol filtering, and source timestamp ordering.
4. Document any missing external venue trade fields.

## Acceptance Criteria

- Trade reports are stable under replayed or duplicated source events.
- Pagination uses deterministic ordering.
- Tests cover core trade data and at least one external/Polymarket-style trade projection if available.

## Read First

- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/reliability-market-data.md](../../ai/maps/reliability-market-data.md)
- [../../ai/maps/polymarket-security.md](../../ai/maps/polymarket-security.md)

