# Phase 25 - Admin Back Office

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Implement the production back-office control plane for user, account, order, trade, and wallet operations with RBAC and four-eyes approval.

## Why this phase exists

Production exchanges need safe operational tooling for review, investigation, and controlled interventions without bypassing ledger and audit requirements.

## Dependencies

- Previous phases required: Phase 21, Phase 22, Phase 24, and relevant trading data phases
- External dependencies if any: RBAC policy model, operator workflow requirements
- Blocking risks: over-privileged admin access, unlogged actions, approval bypasses

## Scope

- Admin RBAC
- User lookup
- Account lookup
- Order lookup
- Trade lookup
- Deposit / withdrawal review
- Asset adjustment request
- Four-eyes approval
- Admin audit log
- Reason code

## Non-goals

- Silent direct balance mutation
- Ad-hoc database editing as an operational substitute
- Replacing reconciliation workflows

## Required deliverables

- Admin permission model
- Lookup APIs and views
- Deposit and withdrawal review tools
- Asset-adjustment request workflow
- Four-eyes approval flow
- Reason-code taxonomy
- Immutable admin audit log
- Admin back-office test suite

## Acceptance criteria

- Every admin action is authenticated and authorized
- Sensitive actions require approval where policy says so
- Asset adjustments go through request and approval workflow
- Audit log records actor, target, reason, before or after state context, and outcome
- Admin tooling does not bypass ledger or reservation boundaries

## Required tests

- RBAC tests
- Lookup authorization tests
- Review workflow tests
- Four-eyes approval tests
- Reason-code enforcement tests
- Admin audit-log tests

## Files / modules likely affected

- admin packages under `server/src/main/java/com/lumix/`
- audit packages
- wallet, order, and account lookup controllers

## Data model impact

- Uses admin audit and review tables
- May add adjustment-request and approval metadata

## API impact

- Introduces production admin-only APIs or internal endpoints
- No change to public claims of full launch readiness

## Security impact

- Critical reliance on RBAC, audit logging, and least privilege
- Must detect and block privilege escalation or approval bypass

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because admin tooling can influence sensitive wallet and funds workflows

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: admin control defects create high-impact abuse paths

## Cannot claim yet

- full risk engine completed
- liquidity controls completed
- launch readiness completed

## Next phase handoff

Phase 26 adds production risk limits, halts, pauses, and kill-switch controls across trading and wallet flows.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/ARCHITECTURE_PRODUCTION.md, and docs/phases/PHASE_25_ADMIN_BACK_OFFICE.md.

Goal: implement Phase 25 only - Admin Back Office.
Scope: admin RBAC, user/account/order/trade lookup, deposit/withdraw review, asset-adjustment requests, four-eyes approval, admin audit log, and reason codes.
Non-goals: silent direct balance mutation, ad-hoc DB edits as operations, later phases.
Deliverables: production admin back office, tests, and progress/doc updates tied to real implementation.
Tests: RBAC, lookup authorization, review workflows, four-eyes approval, reason codes, audit log, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 25 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: full risk engine completed, liquidity controls completed, launch readiness completed.
Final output format: Changed Files, Summary, What Phase 25 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
