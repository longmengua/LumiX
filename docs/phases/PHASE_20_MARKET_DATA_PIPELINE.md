# Phase 20 - Production Market Data Pipeline

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Build the production market-data pipeline from authoritative matching events and settlement-adjacent state.

## Why this phase exists

Production trading cannot be claimed without authoritative order-book snapshots, deltas, trades, ticker, kline, cache, and websocket or REST publication backed by recovery logic.

## Dependencies

- Previous phases required: Phase 17, Phase 18, Phase 19
- External dependencies if any: Redis, websocket transport, REST exposure layer
- Blocking risks: sequence gaps, stale cache, incorrect replay recovery, non-authoritative data publication

## Scope

- Order book snapshot
- Order book delta
- Trade tape
- Ticker
- Kline
- Redis cache
- WebSocket fanout
- REST market API
- Sequence gap handling
- Market data recovery

## Non-goals

- Matching implementation
- User balance mutation
- Wallet flows
- Futures market-data extensions beyond defined scope

## Required deliverables

- Event-to-market-data pipeline
- Redis-backed market-data cache design
- WebSocket fanout path
- REST market-data API exposure
- Sequence-gap detection and recovery behavior
- Replay or rebuild path for snapshots and deltas
- Market-data test suite

## Acceptance criteria

- Market data is derived only from authoritative events
- Order-book snapshots and deltas are consistent under replay
- Trade tape, ticker, and kline recover after restart
- Sequence gaps are detected and handled safely
- REST and WebSocket outputs match internal market-data state

## Required tests

- Snapshot generation tests
- Delta application tests
- Trade tape tests
- Ticker and kline tests
- Sequence gap and recovery tests
- Redis cache and websocket publication tests

## Files / modules likely affected

- `server/src/main/java/com/lumix/market/`
- websocket packages
- REST market API packages
- cache integration

## Data model impact

- May add market-data checkpoints, snapshot metadata, and replay markers
- Redis becomes a key runtime data store for published views

## API impact

- Introduces production REST market APIs and websocket feeds
- Public market APIs become authoritative only after this phase passes

## Security impact

- Must protect websocket and API infrastructure against abuse
- Must isolate public market reads from internal privileged data

## User funds impact

- No direct balance mutation
- Review requirements: mandatory human review before merge because incorrect market data can mislead trading behavior and incident response

## Risk level

- High

## Review gate

- Mandatory human review before merge: Yes
- Why: this phase defines externally visible authoritative market state

## Cannot claim yet

- real deposit completed
- real withdrawal completed
- production wallet completed
- launch readiness completed

## Next phase handoff

Phase 21 implements real inbound deposit processing with idempotent ledger crediting.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/TRADING_CORE_BOUNDARIES.md, docs/ARCHITECTURE_PRODUCTION.md, and docs/phases/PHASE_20_MARKET_DATA_PIPELINE.md.

Goal: implement Phase 20 only - Production Market Data Pipeline.
Scope: order-book snapshot and delta, trade tape, ticker, kline, Redis cache, websocket fanout, REST market API, sequence-gap handling, and recovery.
Non-goals: matching implementation, balance mutation, wallet runtime, later phases.
Deliverables: market-data pipeline, tests, and progress/doc updates tied to real implementation.
Tests: snapshot/delta, trade tape, ticker/kline, sequence gap recovery, cache publication, websocket/REST validation, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 20 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: real deposit completed, real withdrawal completed, production wallet completed, launch readiness completed.
Final output format: Changed Files, Summary, What Phase 20 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
