# Phase 12 - Production Database Schema & Migration

## Status

```text
planned, not started
```

## Goal

Create the production database foundation that later phases can safely build on.

## Out of scope

```text
runtime ledger mutation
runtime balance mutation
matching execution
settlement execution
deposit crediting
withdrawal signing / broadcast
```

## Required reading

```text
AGENTS.md
AI_AGENT.md
docs/ai/AI_CONTEXT_ROUTING.md
docs/backend/transaction-boundary.md
docs/exchange-core/ledger-invariants.md
docs/exchange-core/reservation-state-machine.md
```

## Task order

```text
P12-T01 migration tool and directory conventions
P12-T02 identity, user, account, and asset tables
P12-T03 balance projection tables
P12-T04 ledger journal and entries
P12-T05 order, trade, reservation, settlement schema
P12-T06 deposit, withdrawal, address, chain transaction schema
P12-T07 outbox, audit log, idempotency, admin action schema
P12-T08 constraints, indexes, uniqueness, precision rules
P12-T09 schema verification tests and rollback notes
```

## Done criteria

- Migrations exist.
- Schema can be applied to a clean PostgreSQL database.
- Core constraints and indexes are defined.
- Schema verification exists.
- Rollback / repair strategy is documented.
- No runtime money movement is implemented in this phase.
