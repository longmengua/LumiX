# Phase 15 - Asset Reservation / Freeze Engine

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Implement the production reservation engine that controls available balance, locked balance, and the reserve/release/commit/rollback lifecycle.

## Why this phase exists

Production spot orders and withdrawals cannot safely proceed without a formal freeze engine with idempotency, partial consumption, cancel release, and stuck-state detection.

## Dependencies

- Previous phases required: Phase 12, Phase 13, Phase 14
- External dependencies if any: clear reservation state model and concurrency policy
- Blocking risks: negative available balance, double release, partial-fill bugs, stuck reservations

## Scope

- Reserve
- Release
- Commit
- Rollback
- Locked balance
- Available balance
- Partial fill support
- Cancel release
- Idempotent reservation events
- Stuck reservation detection

## Non-goals

- Order matching
- Spot order orchestration beyond reservation boundary
- Settlement runtime beyond commit/release hooks
- Wallet broadcast logic

## Required deliverables

- Reservation service implementation
- Reservation state machine
- Balance mutation hooks against ledger/projection boundaries
- Idempotent reserve/release/commit/rollback handling
- Partial-fill and cancel-release logic
- Stuck reservation detector
- Reservation test suite

## Acceptance criteria

- Reserve decreases available and increases locked deterministically
- Release and commit are idempotent
- Rollback restores safe state when allowed
- Partial fills consume only the appropriate reservation amount
- Stuck reservations are detectable and reportable

## Required tests

- Reserve/release/commit/rollback tests
- Negative-balance prevention tests
- Idempotency tests
- Partial-fill tests
- Cancel-release tests
- Stuck-reservation detection tests

## Files / modules likely affected

- new reservation package under `server/src/main/java/com/lumix/`
- `server/src/main/java/com/lumix/ledger/`
- `server/src/main/java/com/lumix/account/`

## Data model impact

- Uses reservation tables from Phase 12
- May add reservation event or state-transition metadata

## API impact

- Internal service boundary becomes available to spot order and withdrawal flows
- No public claim of full trading yet

## Security impact

- Must prevent unauthorized release or commit paths
- Must maintain full audit trail for every reservation transition

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because this is the formal production freeze phase

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: reservation defects directly risk overspending or locked-funds loss

## Cannot claim yet

- production spot order flow completed
- matching completed
- settlement completed
- production trading completed

## Next phase handoff

Phase 16 consumes the reservation engine to implement production spot order orchestration without fake matching.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/FUNDS_SAFETY_MODEL.md, docs/ORDER_SETTLEMENT_FLOW.md, docs/ARCHITECTURE_PRODUCTION.md, and docs/phases/PHASE_15_ASSET_RESERVATION.md.

Goal: implement Phase 15 only - Asset Reservation / Freeze Engine.
Scope: reserve, release, commit, rollback, locked and available balance handling, partial fill support, cancel release, idempotency, and stuck reservation detection.
Non-goals: spot order orchestration, matching, settlement, wallet runtime, later phases.
Deliverables: reservation service, state machine, tests, and progress/doc updates tied to real implementation.
Tests: reserve/release/commit/rollback, negative balance prevention, idempotency, partial fill, cancel release, stuck reservation detection, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 15 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: production spot order flow completed, matching completed, settlement completed, production trading completed.
Final output format: Changed Files, Summary, What Phase 15 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
