# Task: Admin Web

Status: `todo`

## Goal

Build an admin/operations web application for market configuration, risk controls, reconciliation, outbox/DLQ operations, liquidation/ADL review, market-maker controls, and audit inspection.

## Scope

- Dashboard for system status, matching ownership, risk switches, DLQ, reconciliation issues, and alerts.
- Market and symbol configuration views.
- Risk parameter and global switch controls.
- Reconciliation report viewer and exception workflow.
- Outbox/DLQ replay and manual compensation workflow.
- Liquidation/ADL queue inspection and operator actions.
- Market-maker inventory, kill switch, hedge status, and audit trail.
- Admin auth and role/scope checks aligned with protected API categories.

## First Implementation Slice

1. Decide frontend location and stack based on the repository state.
2. Add admin app shell with routes for dashboard, risk, reconciliation, outbox, and market-maker operations.
3. Add typed API client wrappers for recovery, risk, ops metrics, and admin-facing endpoints.
4. Implement read-only dashboards first; gate mutating actions behind confirmation and role checks.
5. Add tests or smoke checks for protected routes and critical action dialogs.

## Acceptance Criteria

- Admin can inspect risk switches, reconciliation reports, DLQ records, and ops metrics.
- Mutating actions require confirmation and show trace ids for audit.
- Views distinguish read-only status from operator actions.
- No secret is committed to frontend config.

## Read First

- [../../ai/maps/web-apps.md](../../ai/maps/web-apps.md)
- [../../ai/maps/reliability-market-data.md](../../ai/maps/reliability-market-data.md)
- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/market-maker-hedging.md](../../ai/maps/market-maker-hedging.md)
