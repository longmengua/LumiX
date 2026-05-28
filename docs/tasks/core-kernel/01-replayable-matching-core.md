# Task: Replayable Matching Core

Status: `doing`

## Goal

Evolve the current in-memory matching core toward replayability with durable command log, event log, snapshots, offset checkpoints, and deterministic replay validation.

## Scope

- Define matching command records for submit, cancel, amend, and cancel-replace.
- Add command sequence / offset identity per symbol.
- Persist or model a durable command log boundary.
- Ensure snapshots carry enough checkpoint metadata to replay from the next command.
- Add deterministic replay validation for book state, command/event offsets, match sequence, and emitted trades.

## First Implementation Slice

1. [x] Add domain DTOs for matching command log entries and checkpoints.
2. [x] Add in-memory command log adapter for tests.
3. [x] Add replay API on the matching engine or a matching recovery service.
4. [x] Extend matching tests to rebuild a book from command log plus snapshot.

## Progress

- Added `MatchingCommandLogEntry`, `MatchingCommandType`, and `MatchingCommandLog`.
- Added `InMemoryMatchingCommandLog` for deterministic tests.
- `MatchingEngineSnapshot` now carries `commandOffset` and `eventOffset`.
- `InMemoryMatchingEngine` records submit/cancel/amend commands, records trade events into an in-memory event log, and can replay entries after the snapshot checkpoint.
- `InMemoryMatchingEngineTest` covers snapshot checkpoint replay, FIFO preservation, top-of-book rebuild, and match sequence continuation.
- Added `MatchingReplayValidationReport` and validation API to compare replay output against an expected snapshot.
- Replay validation reports command offset, event offset, match sequence, book-level mismatches, and validation timestamp.
- Added Flyway migration `V7__matching_replay_logs.sql` for durable matching command/event logs and offset checkpoints.
- Added JPA record repositories and adapters (`JpaMatchingCommandLog`, `JpaMatchingEventLog`) so Spring can use durable log storage.
- Durable command/event offsets are allocated through a per-symbol checkpoint row with pessimistic locking.
- Added `MatchingSnapshotStore` and `JpaMatchingSnapshotStore` for durable matching snapshot persistence.
- Added `MatchingReplayValidationReportStore` and JPA adapter for durable recovery audit reports.
- Added `MatchingRecoveryService` to orchestrate worker startup/takeover recovery from latest snapshot plus command log replay.
- Recovery service saves the recovered snapshot and durable replay validation report after deterministic validation.
- Added `MatchingSequencerLeaseStore`, durable lease schema, and `MatchingSequencerLeaseService` for per-symbol owner / epoch lifecycle.
- Added `CANCEL_REPLACE` matching command semantics with replacement order payload and replay coverage.
- Added `MatchingSequencerLeaseService.requireWritable(...)` guard to reject missing lease, wrong owner, stale epoch, and expired lease before command writes.
- Added owner epoch audit fields to matching command/event logs and append APIs.

Remaining work:
- Add stronger application/accounting atomicity around cancel-replace reserve release and replacement reserve.
- Wire lease write guard into the production worker command pipeline.

## Acceptance Criteria

- Tests prove a new engine can restore from snapshot and replay subsequent commands.
- Replay preserves FIFO, match sequence, and resulting top-of-book.
- Replay validation detects command offset, event offset, match sequence, and book-level mismatches.
- Task docs and AI maps are updated after implementation.

## Read First

- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
