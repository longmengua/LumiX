# Task: Admin Reconciliation Report Screen

Status: `todo`

## Goal

Add an admin screen for reconciliation reports, issue workflow, trial-balance summaries, and operator follow-up.

## Scope

- Report list filtered by date, report type, status, severity, owner, and unresolved issues.
- Report detail with totals, mismatches, issue workflow fields, and audit history.
- Operator workflow actions for assign, comment, resolve, and reopen when backend APIs support them.
- Links from report rows to related ledger, order, trade, or outbox identifiers when available.

## First Implementation Slice

1. Map reconciliation report and issue workflow APIs.
2. Build read-only report list/detail routes.
3. Add issue status badges and owner/resolution metadata.
4. Add disabled workflow actions until permission and backend support are confirmed.
5. Test filtering and issue status rendering.

## Acceptance Criteria

- Operators can inspect reports and unresolved reconciliation issues.
- Workflow actions show confirmation, actor, reason/comment, and resulting trace id.
- Report views do not require direct database access.

## Read First

- [../web/02-admin-web.md](../web/02-admin-web.md)
- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)

