# Task: Admin Market Config Screen

Status: `done`

## Goal

Add an admin screen for inspecting and editing market, symbol, and trading-session configuration with clear auditability and safe rollout behavior.

## Scope

- Read-only market list with symbol, status, price precision, quantity precision, min order size, and trading mode.
- Detail view for market metadata, session windows, matching enablement, market-data enablement, and manual suspension state.
- Mutating actions for safe config changes only after backend API capability and permission checks exist.
- Operator audit trail with request id, actor, before/after summary, and reason.

## First Implementation Slice

1. Confirm admin frontend location from [../web/02-admin-web.md](../web/02-admin-web.md).
2. Add typed API contract or mock adapter for market-config reads.
3. Build read-only list/detail routes first.
4. Add disabled mutating controls that reveal missing backend permissions/API gaps.
5. Document required backend endpoints before enabling writes.

## Acceptance Criteria

- Operators can inspect market configuration without mutating state.
- Missing backend write support is represented as disabled controls, not hidden assumptions.
- Any enabled write requires confirmation, reason text, permission check, and trace id display.

## Implementation Notes

- Read-only API: `GET /api/admin/market-config`.
- Static page: `src/main/resources/static/admin-market-config.html`.
- Curl script: `shells/api-curls/exchange/admin-market-config-get.sh`.
- Writes remain disabled and documented through response capabilities.

## Read First

- [../web/02-admin-web.md](../web/02-admin-web.md)
- [../../ai/maps/web-apps.md](../../ai/maps/web-apps.md)
- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
