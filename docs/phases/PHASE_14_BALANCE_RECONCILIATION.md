# Phase 14 - Balance Projection & Ledger Reconciliation

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Project user balances from ledger journals and detect any divergence between journal state and balance state.

## Why this phase exists

The ledger is the source of truth, but production operations need fast balance reads, rebuild capability, and automated detection of imbalances or stuck journal flows.

## Dependencies

- Previous phases required: Phase 12, Phase 13
- External dependencies if any: reporting requirements, operational alerting destination
- Blocking risks: projection drift, incomplete rebuild path, silent mismatch handling

## Scope

- Ledger journal to balance projection
- Rebuild projection from journal history
- Reconciliation run
- Ledger vs balance check
- Imbalance detection
- Stuck journal detection
- Audit reports
- No automatic asset repair without approval

## Non-goals

- Reservation runtime
- Order orchestration
- Matching runtime
- Settlement runtime
- Automatic asset repair

## Required deliverables

- Balance projection service
- Rebuild tooling
- Reconciliation report format
- Imbalance and stuck-journal detection
- Audit report outputs
- Operational guidance for manual follow-up

## Acceptance criteria

- Balance views can be rebuilt from journals
- Rebuilt balances match current projected balances in normal cases
- Ledger-vs-balance mismatches are surfaced deterministically
- Stuck journal states are detectable and reportable
- No automatic repair path mutates assets without approval

## Required tests

- Projection update tests
- Full rebuild tests
- Ledger-vs-balance mismatch detection tests
- Stuck journal detection tests
- Audit report generation tests

## Files / modules likely affected

- `server/src/main/java/com/lumix/ledger/`
- future `server/src/main/java/com/lumix/reconciliation/`
- reporting or ops-support packages

## Data model impact

- May add projection tables, reconciliation snapshots, and reporting metadata

## API impact

- Enables future account-balance and audit read APIs
- No public trading API claims yet

## Security impact

- Must keep report access limited to authorized operators and auditors
- Must not allow automatic repair without explicit approval workflow

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because balance projection errors can misstate available or locked funds

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: reconciliation and projection correctness are core funds-safety controls

## Cannot claim yet

- reservation/freeze completed
- production spot order flow completed
- matching completed
- settlement completed
- automatic asset repair approved

## Next phase handoff

Phase 15 consumes projected balances to implement reserve, release, commit, rollback, and stuck-reservation detection.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/FUNDS_SAFETY_MODEL.md, docs/ARCHITECTURE_PRODUCTION.md, docs/phases/PHASE_13_DOUBLE_ENTRY_LEDGER.md, and docs/phases/PHASE_14_BALANCE_RECONCILIATION.md.

Goal: implement Phase 14 only - Balance Projection & Ledger Reconciliation.
Scope: projection from journals, rebuild tooling, ledger-vs-balance checks, imbalance detection, stuck journal detection, and audit reports.
Non-goals: reservation runtime, matching, settlement, wallet runtime, automatic asset repair, later phases.
Deliverables: projection implementation, rebuild tooling, reconciliation checks, tests, and progress/doc updates tied to actual code.
Tests: projection correctness, rebuild, mismatch detection, stuck journal detection, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 14 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: freeze completed, production spot order flow completed, matching completed, settlement completed.
Final output format: Changed Files, Summary, What Phase 14 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
