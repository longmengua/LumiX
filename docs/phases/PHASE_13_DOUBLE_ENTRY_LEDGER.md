# Phase 13 - Double-Entry Ledger Engine

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Implement the immutable double-entry ledger engine as the only authority for balance-affecting asset mutation.

## Why this phase exists

All later funds and settlement flows depend on balanced journals, idempotent posting, reversal semantics, and a financial audit trail.

## Dependencies

- Previous phases required: Phase 12
- External dependencies if any: finalized schema, journal account taxonomy, accounting review input
- Blocking risks: unbalanced posting rules, weak idempotency, poor concurrency handling, unclear reversal model

## Scope

- Double-entry posting
- Debit and credit validation
- Journal immutability
- Idempotency
- Account balance projection update boundary
- Negative balance prevention hooks
- Rollback or reversal model
- Financial audit trail
- Concurrency handling
- Ledger tests

## Non-goals

- Reservation runtime
- Order orchestration
- Matching runtime
- Settlement runtime
- Wallet runtime

## Required deliverables

- `LedgerService` production implementation
- Journal and line validation rules
- Idempotent posting boundary
- Reversal model
- Concurrency control design
- Audit-friendly journal records
- Ledger test suite

## Acceptance criteria

- Every posted journal balances
- Duplicate request IDs do not double-post
- Journals become immutable after commit
- Reversal behavior is explicit and auditable
- Ledger engine fails closed on invalid debit or credit requests

## Required tests

- Balanced posting tests
- Invalid debit/credit rejection tests
- Duplicate request idempotency tests
- Concurrency tests
- Reversal tests
- Audit trail integrity tests

## Files / modules likely affected

- `server/src/main/java/com/lumix/ledger/`
- `server/src/main/java/com/lumix/account/`
- repository packages
- DB-backed ledger tests

## Data model impact

- Uses journals and lines from Phase 12
- May add ledger metadata fields or supporting lookup tables if schema review requires them

## API impact

- No direct public API required
- Internal asset-changing services begin to depend on the ledger boundary

## Security impact

- Must preserve immutability and auditability
- Must prevent unauthorized direct mutation paths around the ledger

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because this phase defines the authoritative funds mutation engine

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: ledger defects directly threaten correctness of user funds

## Cannot claim yet

- order freeze completed
- spot order production flow completed
- matching completed
- settlement completed

## Next phase handoff

Phase 14 consumes journal output to build balance projection, rebuild, and reconciliation checks.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/ARCHITECTURE_PRODUCTION.md, docs/FUNDS_SAFETY_MODEL.md, docs/phases/PHASE_12_DATABASE_SCHEMA.md, and docs/phases/PHASE_13_DOUBLE_ENTRY_LEDGER.md.

Goal: implement Phase 13 only - Double-Entry Ledger Engine.
Scope: double-entry posting, debit/credit validation, journal immutability, idempotency, reversal model, concurrency handling, and audit trail.
Non-goals: reservation runtime, matching, settlement, wallet runtime, later phases.
Deliverables: LedgerService implementation, persistence layer, ledger tests, and docs/progress updates tied to actual implementation.
Tests: balanced journal tests, invalid posting rejection, idempotency, reversal, concurrency, and build validation.
Docs to update: AI_PROGRESS.md plus the Phase 13 doc if implementation details materially change.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: order freeze completed, spot order production flow completed, matching completed, settlement completed.
Final output format: Changed Files, Summary, What Phase 13 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
