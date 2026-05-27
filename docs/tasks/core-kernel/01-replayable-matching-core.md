# Task: Replayable Matching Core

Status: `todo`

## Goal

Evolve the current in-memory matching core toward replayability with durable command log, event log, snapshots, offset checkpoints, and deterministic replay validation.

## Scope

- Define matching command records for submit, cancel, amend, and cancel-replace.
- Add command sequence / offset identity per symbol.
- Persist or model a durable command log boundary.
- Ensure snapshots carry enough checkpoint metadata to replay from the next command.
- Add deterministic replay validation for book state, match sequence, and emitted trades.

## First Implementation Slice

1. Add domain DTOs for matching command log entries and checkpoints.
2. Add in-memory command log adapter for tests.
3. Add replay API on the matching engine or a matching recovery service.
4. Extend matching tests to rebuild a book from command log plus snapshot.

## Acceptance Criteria

- Tests prove a new engine can restore from snapshot and replay subsequent commands.
- Replay preserves FIFO, match sequence, and resulting top-of-book.
- Task docs and AI maps are updated after implementation.

## Read First

- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
