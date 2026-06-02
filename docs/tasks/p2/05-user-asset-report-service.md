# Task: User Asset Report Service

Status: `todo`

## Goal

Add a backend report service for user asset balances, wallet ledger summaries, bonus-credit balances, and position exposure.

## Scope

- Query user asset snapshot by uid and reporting date/time window.
- Include cash balance, locked balance, bonus-credit balance, open position exposure, realized PnL, and unsettled movements where available.
- Use ledger and position projections rather than ad hoc balance calculations.
- Provide export-ready DTOs with stable field names.

## First Implementation Slice

1. Identify existing wallet ledger, account, bonus, and position projection sources.
2. Add service DTOs and a read-only service method for one uid/time window.
3. Add deterministic tests using existing ledger/position fixtures.
4. Add an admin-facing endpoint only if existing security classifications are clear.
5. Document data gaps instead of fabricating unavailable fields.

## Acceptance Criteria

- Report output is reproducible for the same input data and window.
- Balance fields identify their source and whether they are snapshot or derived.
- Tests cover locked funds, bonus credit, and at least one open-position exposure case.

## Read First

- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)

