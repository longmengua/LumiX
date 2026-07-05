# Phase 17 - C++ Matching Core

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Implement the deterministic production C++ matching engine and authoritative order book.

## Why this phase exists

Production trading cannot be claimed while Java only exposes `MatchingEngineClient` without a real deterministic matching core, replay path, snapshot path, or crash recovery behavior.

## Dependencies

- Previous phases required: Phase 11, with Phase 16 defining the Java-side submission boundary
- External dependencies if any: C++ toolchain, protocol contract, performance targets, benchmark environment
- Blocking risks: nondeterminism, sequence gaps, broken crash recovery, incomplete market-order semantics

## Scope

- Deterministic matching engine
- Price-time priority
- Order book
- Limit order
- Market order
- Cancel
- Partial fill
- Sequence number
- Replay
- Snapshot
- Crash recovery
- Benchmark
- C++ tests

## Non-goals

- Balance mutation
- Ledger writes
- Wallet operations
- Admin adjustments
- Settlement logic

## Required deliverables

- `core/` or `matching-core/` source tree
- Deterministic matching core implementation
- Authoritative order-book state model
- Snapshot and replay tooling
- Crash recovery behavior
- Performance benchmarks
- C++ test suite

## Acceptance criteria

- Matching is deterministic under replay
- Sequence numbers are monotonic and authoritative
- Limit, market, cancel, and partial-fill behavior are defined and tested
- Snapshot and recovery produce consistent state
- Benchmark evidence exists for expected throughput and latency targets

## Required tests

- Deterministic replay tests
- Price-time priority tests
- Limit and market order tests
- Cancel tests
- Partial-fill tests
- Snapshot and crash-recovery tests
- Benchmark runs with captured results

## Files / modules likely affected

- new `core/` or `matching-core/`
- shared protocol documentation
- build tooling for the core

## Data model impact

- No direct Java DB writes
- Defines authoritative event payload shape consumed by later phases

## API impact

- Introduces core command and event contracts for later integration
- No user-facing Java API completion claim yet

## Security impact

- Must isolate the core from direct balance or wallet mutation paths
- Must preserve replay integrity and event authenticity expectations

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because matching determinism and execution correctness directly affect user outcomes

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: matching defects create incorrect execution state even before settlement

## Cannot claim yet

- Java integration completed
- settlement completed
- production market data completed
- production trading completed

## Next phase handoff

Phase 18 integrates Java order orchestration with the new C++ command and event protocols.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/TRADING_CORE_BOUNDARIES.md, docs/ARCHITECTURE_PRODUCTION.md, and docs/phases/PHASE_17_CPP_MATCHING_CORE.md.

Goal: implement Phase 17 only - C++ Matching Core.
Scope: deterministic matching engine, price-time priority, order book, limit/market/cancel/partial fill support, sequence numbers, replay, snapshots, crash recovery, benchmarks, and C++ tests.
Non-goals: ledger mutation, wallet operations, settlement, Java integration runtime beyond protocol contracts, later phases.
Deliverables: core source tree, tests, benchmarks, protocol docs, and progress/doc updates tied to actual implementation.
Tests: deterministic replay, price-time priority, order type handling, cancel, partial fill, snapshot/recovery, benchmarks, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 17 doc only if implementation changes reality.
Validation commands: run the matching-core test/build commands you add, plus cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: Java integration completed, settlement completed, production market data completed, production trading completed.
Final output format: Changed Files, Summary, What Phase 17 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
