# Task: KYC AML Sanctions Integration

Status: `todo`

## Goal

Add a compliance integration baseline for KYC status, AML risk status, sanctions screening, and account action gating.

## Scope

- Domain model for compliance status and last screening metadata.
- Provider adapter contract for KYC/AML/sanctions lookup without committing provider secrets or real payloads.
- Gating rules for account activation, withdrawal, high-risk operations, and manual review.
- Audit events for status changes and provider outcomes.

## First Implementation Slice

1. Define compliance status states and account gating decisions.
2. Add provider-neutral DTOs and adapter contract.
3. Add service tests for allowed, blocked, pending, and manual-review states.
4. Document secret/environment requirements and payload redaction rules.
5. Add endpoint or scheduler only after security classification is clear.

## Acceptance Criteria

- No provider secrets, production customer data, or real sanctions payloads are committed.
- Account gating decisions are deterministic and auditable.
- Tests cover status transitions and blocked-operation decisions.

## Read First

- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/web-apps.md](../../ai/maps/web-apps.md)

