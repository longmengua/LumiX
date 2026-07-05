# Phase 18 - Java ↔ C++ Core Integration

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Implement the production integration boundary between Java order orchestration and the C++ matching core.

## Why this phase exists

Even with a real matching core, production trading still fails without a reliable command path, event path, replay handling, duplicate handling, and backpressure or circuit-breaker controls.

## Dependencies

- Previous phases required: Phase 16, Phase 17
- External dependencies if any: transport choice, protocol serialization, integration environment
- Blocking risks: duplicate or missing events, sequence drift, blocked command path, uncontrolled backpressure

## Scope

- Command protocol
- Event protocol
- Submit order
- Cancel order
- Fill event consumer
- Sequence guarantee
- Duplicate event handling
- Replay handling
- Backpressure
- Circuit breaker
- Integration tests

## Non-goals

- Matching-core implementation itself
- Settlement runtime
- Market-data runtime
- Wallet runtime

## Required deliverables

- `MatchingEngineClient` production implementation
- Command and event protocol contracts
- Order submit and cancel integration path
- Fill-event consumer boundary
- Sequence and replay policy
- Backpressure and circuit-breaker behavior
- Integration test suite

## Acceptance criteria

- Java can submit and cancel orders through the core boundary safely
- Duplicate events do not double-apply downstream effects
- Replay path can recover missing state
- Sequence guarantees are documented and enforced
- Backpressure and circuit-breaker behavior fails closed

## Required tests

- Submit integration tests
- Cancel integration tests
- Duplicate event handling tests
- Replay recovery tests
- Sequence gap handling tests
- Backpressure and circuit-breaker tests

## Files / modules likely affected

- `server/src/main/java/com/lumix/spot/`
- new integration packages
- shared protocol docs
- possible `core/` protocol modules

## Data model impact

- May add integration event checkpoint metadata and replay markers

## API impact

- No new end-user API surface required
- Existing order APIs gain a real matching path behind the boundary

## Security impact

- Must authenticate or trust-boundary protect the Java-to-core channel
- Must guard against malformed or replayed integration messages

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because integration defects can misroute execution and downstream settlement

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: protocol and sequencing defects can corrupt the trading pipeline

## Cannot claim yet

- settlement completed
- production market data completed
- production trading completed

## Next phase handoff

Phase 19 consumes authoritative fill events to perform fee calculation, reserve commit, unused reserve release, and final state updates.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/TRADING_CORE_BOUNDARIES.md, docs/ORDER_SETTLEMENT_FLOW.md, docs/phases/PHASE_17_CPP_MATCHING_CORE.md, and docs/phases/PHASE_18_MATCHING_INTEGRATION.md.

Goal: implement Phase 18 only - Java ↔ C++ Core Integration.
Scope: command protocol, event protocol, submit/cancel integration, fill event consumption, sequence guarantees, duplicate handling, replay handling, backpressure, circuit breaker, and integration tests.
Non-goals: matching-core implementation, settlement runtime, market-data runtime, wallet runtime, later phases.
Deliverables: MatchingEngineClient implementation, protocol contracts, integration tests, and progress/doc updates tied to real implementation.
Tests: submit/cancel integration, duplicate handling, replay recovery, sequence gap handling, backpressure, circuit breaker, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 18 doc only if implementation changes reality.
Validation commands: run matching-core validation as needed, plus cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: settlement completed, production market data completed, production trading completed.
Final output format: Changed Files, Summary, What Phase 18 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
