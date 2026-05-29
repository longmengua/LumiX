# Task: Production Worker Routing

Status: `todo`

## Goal

Route matching commands through the sequencer lease guard and owner epoch before live writes so only the active owner for a symbol can append or process commands.

## Scope

- Worker command intake and dispatch.
- Lease acquisition, renewal, and expiration behavior.
- `requireWritable` enforcement before command append.
- Rejection of missing owner, wrong owner, stale epoch, and expired lease.
- Owner and epoch audit fields on command/event logs.

## First Implementation Slice

1. Add an orchestration wrapper that requires a valid symbol lease before command append.
2. Wire one production-like command path through the wrapper.
3. Add tests for missing lease, wrong owner, stale epoch, expired lease, and valid owner behavior.
4. Comment tests around the split-brain prevention rule so future maintainers understand the invariant.

## Acceptance Criteria

- A matching command cannot be appended by a worker that does not own the current symbol lease.
- Command/event audit fields preserve owner and epoch.
- Tests prove stale owners are rejected before durable command writes.

## Read First

- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/reliability-market-data.md](../../ai/maps/reliability-market-data.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
