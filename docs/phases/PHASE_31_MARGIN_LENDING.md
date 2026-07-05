# Phase 31 - Margin Lending System

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Implement the production borrow, repay, interest, collateral valuation, and bad-debt controls for margin lending.

## Why this phase exists

Margin trading requires a separate lending and collateral engine beyond spot reservation and beyond futures margin accounting.

## Dependencies

- Previous phases required: Phase 13, Phase 14, Phase 15, Phase 26
- External dependencies if any: collateral policy, lending inventory policy, approved interest model
- Blocking risks: incorrect interest accrual, under-collateralized borrowing, weak forced-repayment flow

## Scope

- Borrow
- Repay
- Interest accrual
- Collateral valuation
- Margin level
- Forced repayment
- Borrow limit
- Lending ledger
- Bad debt handling

## Non-goals

- Futures liquidation logic
- Treasury sweep logic
- Launch-readiness work

## Required deliverables

- Margin lending engine
- Borrow and repay workflows
- Interest-accrual job or engine
- Collateral valuation and margin-level checks
- Borrow-limit enforcement
- Forced-repayment path
- Lending-ledger integration
- Margin lending test suite

## Acceptance criteria

- Borrow and repay flows are journaled and auditable
- Interest accrues deterministically and reproducibly
- Collateral valuation and margin level block unsafe borrowing
- Forced repayment and bad-debt handling are explicit and tested
- Borrow limits are enforceable by asset and account state

## Required tests

- Borrow and repay tests
- Interest-accrual tests
- Collateral valuation tests
- Margin-level tests
- Borrow-limit tests
- Forced-repayment tests
- Bad-debt handling tests

## Files / modules likely affected

- margin packages under `server/src/main/java/com/lumix/`
- `server/src/main/java/com/lumix/ledger/`
- risk and account packages

## Data model impact

- Adds borrow positions, interest accrual records, collateral state, lending-ledger metadata, and bad-debt records

## API impact

- Enables borrow, repay, and lending history APIs for margin accounts

## Security impact

- Must prevent unauthorized borrow or repay operations
- Must audit all lending and forced-repayment actions

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because lending defects create debt, collateral, and solvency risk

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: margin lending directly affects borrow exposure and platform loss paths

## Cannot claim yet

- full reconciliation and compensation completed
- security/compliance hardening completed
- launch readiness completed

## Next phase handoff

Phase 32 adds cross-domain reconciliation and compensation workflows spanning ledger, orders, matching, wallet, and fee revenue.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/ARCHITECTURE_PRODUCTION.md, and docs/phases/PHASE_31_MARGIN_LENDING.md.

Goal: implement Phase 31 only - Margin Lending System.
Scope: borrow, repay, interest accrual, collateral valuation, margin level, forced repayment, borrow limits, lending ledger, and bad debt handling.
Non-goals: futures liquidation logic, treasury sweep logic, later phases.
Deliverables: margin lending system, tests, and progress/doc updates tied to real implementation.
Tests: borrow/repay, interest accrual, collateral valuation, margin level, borrow limits, forced repayment, bad debt handling, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 31 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: full reconciliation and compensation completed, security/compliance hardening completed, launch readiness completed.
Final output format: Changed Files, Summary, What Phase 31 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
