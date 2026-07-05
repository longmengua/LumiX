# Phase 32 - Reconciliation & Compensation System

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Implement the production reconciliation and compensation framework across ledger, balances, orders, matching events, wallet state, and fee revenue.

## Why this phase exists

Large-scale exchange operation requires continuous mismatch detection and controlled repair workflows rather than silent state edits.

## Dependencies

- Previous phases required: Phase 14, Phase 19, Phase 20, Phase 21, Phase 22, and any live product phases in scope
- External dependencies if any: chain data source access, reporting destination, ops approval workflow
- Blocking risks: incomplete comparison coverage, unsafe compensation path, silent auto-repair

## Scope

- Ledger vs balance reconciliation
- Order vs trade reconciliation
- Matching event vs DB reconciliation
- Wallet vs chain reconciliation
- Deposit / withdrawal reconciliation
- Fee revenue reconciliation
- Stuck state detector
- Compensation workflow
- No automatic asset repair without approval

## Non-goals

- Silent direct asset repair
- Replacing ledger, reservation, or settlement engines
- Legal or customer communications beyond workflow hooks

## Required deliverables

- Reconciliation job suite
- Cross-domain mismatch reports
- Stuck-state detectors
- Compensation workflow with approval gates
- Audit trail for reconciliation findings and fixes
- Reconciliation and compensation test suite

## Acceptance criteria

- Mismatches across core domains can be detected deterministically
- Compensation requires explicit approval
- No automatic asset repair runs without approval
- Stuck states are surfaced with actionable evidence
- Reconciliation history is auditable and reportable

## Required tests

- Ledger-vs-balance reconciliation tests
- Order-vs-trade reconciliation tests
- Matching-event-vs-DB reconciliation tests
- Wallet-vs-chain reconciliation tests
- Fee-revenue reconciliation tests
- Compensation approval-flow tests
- Stuck-state detection tests

## Files / modules likely affected

- `server/src/main/java/com/lumix/reconciliation/`
- `server/src/main/java/com/lumix/ledger/`
- wallet, spot, settlement, futures, and admin packages

## Data model impact

- Adds reconciliation runs, mismatch cases, stuck-state records, and compensation-case metadata

## API impact

- Primarily admin and ops-facing
- No public product expansion claim by itself

## Security impact

- Must restrict access to reconciliation findings and compensation tools
- Must preserve maker-checker controls for any asset-affecting compensation

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because compensation workflows can alter user funds under approved conditions

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: this phase governs incident repair and discrepancy handling

## Cannot claim yet

- security/compliance hardening completed
- observability/SRE completed
- production infra/release completed
- launch readiness completed

## Next phase handoff

Phase 33 hardens secrets, abuse detection, compliance hooks, anomaly detection, and security remediation.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/ARCHITECTURE_PRODUCTION.md, and docs/phases/PHASE_32_RECONCILIATION_COMPENSATION.md.

Goal: implement Phase 32 only - Reconciliation & Compensation System.
Scope: ledger-vs-balance, order-vs-trade, matching-event-vs-DB, wallet-vs-chain, deposit/withdrawal, fee-revenue reconciliation, stuck-state detection, compensation workflow, and no automatic asset repair without approval.
Non-goals: silent asset repair, later phases.
Deliverables: reconciliation and compensation system, tests, and progress/doc updates tied to real implementation.
Tests: each reconciliation family, stuck-state detection, compensation approval flow, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 32 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: security/compliance hardening completed, observability/SRE completed, production infra/release completed, launch readiness completed.
Final output format: Changed Files, Summary, What Phase 32 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
