# Phase 12 - Production Database Schema & Migration

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Define and version the canonical production database schema for trading, funds, wallet, reconciliation, and audit domains.

## Why this phase exists

Later phases cannot safely implement ledger, reservation, orders, settlement, or wallet flows without durable schema ownership, migration order, and data invariants.

## Dependencies

- Previous phases required: Phase 11
- External dependencies if any: PostgreSQL target version, migration tool choice, schema review input from ledger/reservation/order/wallet domains
- Blocking risks: ambiguous table ownership, weak uniqueness rules, missing foreign keys, migration order drift

## Scope

- Define tables for accounts, assets, asset networks, symbols, account balances, ledger journals, ledger lines, reservations, orders, trades/fills, deposits, withdrawals, reconciliation, and admin audit
- Define keys, constraints, indexes, enums, timestamps, and archival or partition considerations
- Define migration ordering, rollback approach, and migration test requirements

## Non-goals

- Ledger engine implementation
- Balance mutation logic
- Reservation runtime logic
- Matching runtime logic
- Settlement runtime logic
- Public API runtime logic

## Required deliverables

- Production schema specification
- Migration files and naming convention
- Table ownership map
- Constraint and index plan
- Migration test plan
- Schema documentation for downstream phases

## Acceptance criteria

- A clean database can be created from migrations only
- Required tables for funds, orders, wallet, reconciliation, and audit exist
- Critical uniqueness and foreign-key constraints are defined
- Migration ordering is deterministic and reviewable
- Schema ownership is explicit per domain

## Required tests

- Empty database bootstrap test
- Migration replay test from zero to latest
- Constraint validation tests
- Index existence checks for critical lookup paths
- Migration smoke test in CI-compatible environment

## Files / modules likely affected

- `server/src/main/resources/db/migration/`
- `server/docs/`
- future repository packages under `server/src/main/java/`

## Data model impact

- Introduces canonical persistent structures for all production domains
- Defines how balances, journals, reservations, orders, fills, deposits, withdrawals, reconciliation cases, and admin audit records are stored

## API impact

- No production API behavior yet
- Enables future API contracts to map to durable entities

## Security impact

- Must define audit columns, soft-delete policy where needed, and least-privilege data boundaries
- Must avoid storing secrets or sensitive material in unsafe plaintext fields

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because schema mistakes can corrupt future funds logic and auditability

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: schema mistakes cascade into all later funds and trading phases

## Cannot claim yet

- ledger engine completed
- balance mutation completed
- freeze completed
- matching completed
- settlement completed
- production trading completed

## Next phase handoff

Phase 13 consumes the schema to implement immutable double-entry posting, idempotency, and reversal rules.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/ARCHITECTURE_PRODUCTION.md, docs/FUNDS_SAFETY_MODEL.md, and docs/phases/PHASE_12_DATABASE_SCHEMA.md.

Goal: implement Phase 12 only - Production Database Schema & Migration.
Scope: production schema and migration files for accounts, assets, asset networks, symbols, account balances, ledger journals, ledger lines, reservations, orders, trades/fills, deposits, withdrawals, reconciliation, and admin audit.
Non-goals: ledger engine, reservation runtime, matching, settlement, wallet runtime, later phases.
Deliverables: migration files, schema docs, migration tests, updated AI_PROGRESS.md and roadmap metadata if needed.
Tests: migration bootstrap, replay, constraint checks, and project build validation.
Docs to update: AI_PROGRESS.md and the relevant phase docs only if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: ledger engine completed, freeze completed, matching completed, settlement completed, production trading completed.
Final output format: Changed Files, Summary, What Phase 12 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
