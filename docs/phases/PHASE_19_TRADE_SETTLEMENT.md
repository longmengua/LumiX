# Phase 19 - Trade Settlement Engine

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Consume authoritative fill events and convert them into fee calculation, reserve commit, unused reserve release, ledger entries, and final order-state transitions.

## Why this phase exists

Matching alone does not move assets. Production trading requires deterministic post-fill settlement with idempotency, compensation handling, and durable journal output.

## Dependencies

- Previous phases required: Phase 13, Phase 14, Phase 15, Phase 16, Phase 18
- External dependencies if any: fee schedule policy, fill-event schema, audit and reconciliation requirements
- Blocking risks: duplicate settlement, incorrect fee math, reserve over-commit, failed compensation paths

## Scope

- Fill event settlement
- Maker/taker fee
- Reserve commit
- Unused reserve release
- Ledger entries
- Settlement journal
- Idempotency
- Failed settlement compensation
- Order final state update

## Non-goals

- Matching implementation
- Market-data fanout
- Wallet chain operations
- Futures settlement

## Required deliverables

- Settlement engine implementation
- Fee calculation boundary
- Fill-event consumer logic
- Reservation commit and release hooks
- Settlement journal records
- Failed-settlement compensation workflow
- Settlement test suite

## Acceptance criteria

- Duplicate fill events do not double-settle
- Reserve commit and unused release are deterministic
- Ledger entries reflect buyer, seller, and fee legs
- Failed settlement paths are visible and compensable
- Final order state updates are derived from authoritative events

## Required tests

- Settlement idempotency tests
- Maker/taker fee tests
- Reserve commit and release tests
- Duplicate fill handling tests
- Failed settlement compensation tests
- Final order-state update tests

## Files / modules likely affected

- new `server/src/main/java/com/lumix/settlement/`
- `server/src/main/java/com/lumix/ledger/`
- `server/src/main/java/com/lumix/spot/`

## Data model impact

- Uses fills, reservations, orders, and journals from prior phases
- May add settlement tracking and compensation-case metadata

## API impact

- No direct public API completion claim
- Enables user order status and fill history to reflect settled reality

## Security impact

- Must prevent unauthorized manual settlement mutation
- Must preserve full traceability between fill, reservation, journal, and final order state

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because this phase moves user assets in response to fills

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: settlement correctness is a core funds-safety gate

## Cannot claim yet

- production market data completed
- real deposit completed
- real withdrawal completed
- production trading completed

## Next phase handoff

Phase 20 consumes matching and settlement events to build public and internal market data.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/FUNDS_SAFETY_MODEL.md, docs/ORDER_SETTLEMENT_FLOW.md, docs/TRADING_CORE_BOUNDARIES.md, and docs/phases/PHASE_19_TRADE_SETTLEMENT.md.

Goal: implement Phase 19 only - Trade Settlement Engine.
Scope: fill-event settlement, maker/taker fee calculation, reserve commit, unused reserve release, ledger entries, settlement journal, idempotency, failed settlement compensation, and order final state update.
Non-goals: matching implementation, market-data runtime, wallet runtime, futures runtime, later phases.
Deliverables: settlement engine, tests, and progress/doc updates tied to real implementation.
Tests: settlement idempotency, fee logic, reserve commit/release, duplicate fill handling, compensation flow, final order-state update, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 19 doc only if implementation changes reality.
Validation commands: run matching-core validation as needed, plus cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: production market data completed, real deposit completed, real withdrawal completed, production trading completed.
Final output format: Changed Files, Summary, What Phase 19 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
