# Task: Admin Risk Parameter Screen

Status: `todo`

## Goal

Add an admin screen for risk tiers, global risk switches, liquidation controls, and margin parameter inspection.

## Scope

- Read-only views for margin tiers, leverage caps, position caps, oracle state, and liquidation halt state.
- Operator actions for global halt/manual-review toggles only when backend APIs support permissioned updates.
- Clear separation between account-level inspection and system-level risk settings.
- Audit event visibility for parameter changes and liquidation control actions.

## First Implementation Slice

1. Map existing risk APIs and DTOs.
2. Add read-only risk parameter and switch dashboard.
3. Add typed client methods with explicit empty/error/loading states.
4. Gate mutating controls behind permission checks and confirmation dialogs.
5. Add route smoke tests or component tests for critical status rendering.

## Acceptance Criteria

- Admin can inspect current risk tiers and global risk switches.
- Mutating actions cannot be triggered without explicit confirmation and operator identity.
- Risk pages show stale or missing oracle data distinctly from normal values.

## Read First

- [../web/02-admin-web.md](../web/02-admin-web.md)
- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)

