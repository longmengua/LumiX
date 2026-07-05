# Phase 22 - Production Withdrawal System

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Implement the production withdrawal system from request intake through reservation, approval, broadcast boundary, tracking, and failed-withdrawal release.

## Why this phase exists

Outbound funds movement is one of the highest-risk exchange operations and requires strict security, review, reservation, fee handling, and rollback controls.

## Dependencies

- Previous phases required: Phase 12, Phase 13, Phase 14, Phase 15, Phase 21
- External dependencies if any: broadcast provider, signer boundary, whitelist policy, risk-review input
- Blocking risks: unauthorized withdrawal, missing release on failure, incorrect fee treatment, unsafe whitelist handling

## Scope

- Withdrawal request
- Available balance check
- Fund reservation
- Approval workflow
- Risk review
- Address whitelist
- Fee deduction
- Broadcast boundary
- Tx status tracking
- Failed withdrawal release

## Non-goals

- Hot/cold treasury strategy
- HSM or MPC implementation details beyond boundary definition
- Automatic compensation outside approved workflows

## Required deliverables

- Production `WithdrawService`
- Withdrawal approval and review flow
- Fee deduction policy integration
- Broadcast boundary and tx tracking
- Failed-withdrawal release path
- Withdrawal test suite

## Acceptance criteria

- Withdrawal request fails closed on insufficient available balance
- Reservation occurs before approval or broadcast
- Whitelist and risk review policies are enforced
- Broadcast state and tx tracking are auditable
- Failed withdrawals release funds safely when policy allows

## Required tests

- Available-balance check tests
- Reservation-before-broadcast tests
- Approval and risk-review tests
- Whitelist tests
- Fee deduction tests
- Failed-withdrawal release tests
- Tx status tracking tests

## Files / modules likely affected

- `server/src/main/java/com/lumix/wallet/`
- `server/src/main/java/com/lumix/ledger/`
- `server/src/main/java/com/lumix/risk/`
- admin review packages

## Data model impact

- Uses withdrawal, reservation, whitelist, and audit structures from earlier phases
- May add tx-state or approval metadata

## API impact

- Enables production withdrawal request and history APIs
- Withdraw API must remain tightly permissioned

## Security impact

- Critical dependence on 2FA, whitelist, risk review, and audit logging
- Must protect the broadcast boundary and tx-status update path

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because this phase can move user assets out of the platform

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: withdrawal safety is a core launch blocker

## Cannot claim yet

- treasury completed
- production wallet completed
- launch readiness completed

## Next phase handoff

Phase 23 adds hot/cold wallet treasury controls, thresholds, batching, signer boundaries, and treasury reconciliation.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/ARCHITECTURE_PRODUCTION.md, docs/FUNDS_SAFETY_MODEL.md, and docs/phases/PHASE_22_WITHDRAWAL_SYSTEM.md.

Goal: implement Phase 22 only - Production Withdrawal System.
Scope: withdrawal request, available-balance check, fund reservation, approval workflow, risk review, whitelist, fee deduction, broadcast boundary, tx tracking, and failed withdrawal release.
Non-goals: treasury strategy, HSM/MPC implementation, later phases.
Deliverables: production withdrawal system, tests, and progress/doc updates tied to real implementation.
Tests: available-balance checks, reservation-before-broadcast, approval/risk review, whitelist, fee deduction, failed withdrawal release, tx tracking, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 22 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: treasury completed, production wallet completed, launch readiness completed.
Final output format: Changed Files, Summary, What Phase 22 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
