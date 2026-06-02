# Task: Admin DLQ Replay Screen

Status: `todo`

## Goal

Add an admin screen for inspecting outbox/DLQ records and initiating permissioned replay or compensation workflows.

## Scope

- DLQ list with type, aggregate id, status, attempts, last error summary, created time, and updated time.
- Detail view with sanitized payload, replay eligibility, compensation notes, and trace/request ids.
- Replay action gated by backend eligibility, role checks, operator reason, and confirmation.
- No raw secrets or sensitive payloads rendered in the UI.

## First Implementation Slice

1. Map existing outbox/DLQ query and replay endpoints.
2. Build read-only DLQ table and detail panel.
3. Add sanitized payload rendering with explicit redaction behavior.
4. Add disabled replay controls until backend action contract is confirmed.
5. Add focused tests for redaction and confirmation flow.

## Acceptance Criteria

- Operators can inspect DLQ records and understand replay eligibility.
- Replay actions require explicit reason, confirmation, and visible trace id.
- Sensitive values are redacted in payload previews and logs.

## Read First

- [../web/02-admin-web.md](../web/02-admin-web.md)
- [../../ai/maps/web-apps.md](../../ai/maps/web-apps.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)

