# Task: Production Worker Routing

Status: `doing`

## Goal

Route matching commands through the sequencer lease guard and owner epoch before live writes so only the active owner for a symbol can append or process commands.

## Scope

- Worker command intake and dispatch.
- Lease acquisition, renewal, and expiration behavior.
- `requireWritable` enforcement before command append.
- Rejection of missing owner, wrong owner, stale epoch, and expired lease.
- Owner and epoch audit fields on command/event logs.

## First Implementation Slice

1. [x] Add an orchestration wrapper that requires a valid symbol lease before command append.
2. [x] Wire one production-like command path through the wrapper.
3. [x] Add tests for missing lease, wrong owner, stale epoch, expired lease, and valid owner behavior.
4. [x] Comment tests around the split-brain prevention rule so future maintainers understand the invariant.

## Progress

- Added `MatchingWorkerCommandRouter` as the production-worker-facing append boundary for matching command and event logs.
- The router calls `MatchingSequencerLeaseService.requireWritable(...)` before appending commands, cancel-replace commands, or matching events.
- Router append methods preserve `ownerId` and `ownerEpoch` in command/event log entries for fencing audit and replay investigation.
- Added `MatchingWorkerExecutionService` for worker submit, cancel, amend, and cancel-replace paths: append lease-fenced command log first, then execute the already-logged command through `MatchingEngine.applyLoggedCommand(...)`.
- `InMemoryMatchingEngine.applyLoggedCommand(...)` executes a logged command without appending a duplicate command and propagates command owner/epoch to matching event logs.
- Added `MatchingWorkerLifecycleService` to start configured symbols by acquiring the lease, running recovery, validating replay, and storing the ready owner/epoch context for command intake.
- Added lifecycle lease renewal/readiness handling: renewals update command/event checkpoints and remove readiness if ownership can no longer be renewed.
- Added `MatchingWorkerCommandRouterTest` for valid owner append, wrong owner rejection, stale epoch rejection, expired lease rejection, and event-log owner epoch persistence.
- Added `MatchingWorkerExecutionServiceTest` proving worker submit appends exactly one fenced command, trade events inherit owner/epoch, cancel removes resting orders, amend updates book price, and cancel-replace appends a single fenced command.
- Added `MatchingWorkerLifecycleServiceTest` proving enabled startup acquires ownership and recovers state, disabled startup is inert, already-owned symbols are rejected, renewal updates checkpoints, and renewal failure removes readiness.
- Added `MatchingWorkerProperties` and `matching-worker.*` dev/prod configuration for worker owner id, owned symbols, lease TTL, and renew interval.
- Added `MatchingWorkerLeaseRenewalScheduler` as the scheduled renewal entry point, gated by `matching-worker.enabled`.
- Exposed matching worker owner/readiness context through recovery admin endpoints under `/api/recovery/matching-worker/contexts`.
- Existing submit, cancel, and amend intake paths now route to `MatchingWorkerExecutionService` when the symbol has a ready worker owner context; otherwise they keep the legacy in-process path.
- Cancel-replace keeps the accounting-safe cancel + replacement-submit flow; under a ready worker context both legs are lease-fenced worker commands, preserving reserve release/re-hold semantics.
- Added `matching-worker.fence-legacy-routing` to reject fallback to the old in-process path for configured symbols that are not worker-ready during cutover.
- Added `MatchingWorkerStartupListener` so `matching-worker.enabled=true` starts configured symbols after Spring application readiness.
- Updated the matching sequencer runbook with worker environment variables, readiness inspection, and deployment switch/rollback sequence.

Remaining work:
- Decide whether this task can close after smoke verification.

## Acceptance Criteria

- A matching command cannot be appended by a worker that does not own the current symbol lease.
- Command/event audit fields preserve owner and epoch.
- Tests prove stale owners are rejected before durable command writes.

## Read First

- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/reliability-market-data.md](../../ai/maps/reliability-market-data.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
