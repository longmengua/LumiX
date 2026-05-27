<!-- File purpose: production deployment and failover rules for per-symbol matching sequencers. Chinese version: ../zh-TW/matching-sequencer-runbook.md. -->
# Matching Sequencer Runbook

This document defines production rules for the per-symbol matching sequencer. The current implementation is still in-process, but production deployments must preserve the same invariant: exactly one active writer may process matching commands for a symbol at a time.

中文版本：[../zh-TW/matching-sequencer-runbook.md](../zh-TW/matching-sequencer-runbook.md)

## Invariant

- A symbol has one active matching owner.
- All commands for that symbol are processed in one total order.
- Ownership must be fenced by epoch or lease token so an old owner cannot write after failover.
- Recovery must load the latest snapshot, replay commands/events after the checkpoint, then accept live traffic.

## Partitioning

- Route order, cancel, amend, and cancel-replace commands by normalized `symbol`.
- Keep all commands for the same symbol on the same sequencer partition.
- Do not shard one symbol across multiple active workers.
- Rebalancing may move a symbol to another worker only after the previous owner is fenced and stopped.

## Ownership Lease

Production workers should acquire a per-symbol lease from a strongly consistent store.

Required lease fields:
- `symbol`
- `ownerId`
- `epoch`
- `expiresAt`
- `lastCheckpoint`

Rules:
- Every command write must include the current `epoch`.
- Storage must reject writes from stale epochs.
- Lease renewal must stop before the worker accepts new commands if the backing store is unreachable.
- Lease TTL must be longer than the normal command processing interval and shorter than the operational failover target.

## Startup

1. Acquire the symbol lease and receive a new `epoch`.
2. Load the latest matching snapshot for the symbol.
3. Read the checkpoint included in that snapshot.
4. Replay command/event log entries after the checkpoint.
5. Publish a readiness signal for that symbol only after replay completes.
6. Start accepting live commands for the symbol.

## Planned Failover

1. Stop routing new commands to the old owner.
2. Wait for in-flight commands to drain.
3. Persist a final snapshot and checkpoint.
4. Release the lease or let the new owner acquire a higher `epoch`.
5. Start the new owner with the startup recovery flow.

## Unplanned Failover

1. Detect missed heartbeats or expired lease.
2. Prevent the old owner from writing by advancing the lease `epoch`.
3. Start a replacement worker.
4. Restore from latest snapshot and replay logs after its checkpoint.
5. Resume command routing only after recovery readiness is visible.
6. Audit any commands accepted by clients during the failover window against the command log.

## Operational Controls

- Alert when a symbol has no active owner.
- Alert when more than one owner claims the same symbol.
- Alert when checkpoint lag keeps growing.
- Alert when replay fails or snapshot age exceeds the recovery target.
- Expose per-symbol owner, epoch, checkpoint, replay lag, and halted/running status.

## Current Gap

The current `InMemoryMatchingEngine` provides only an in-process sequencer and snapshot export/restore baseline. It does not yet provide durable command logs, durable event logs, epoch-fenced writes, distributed leases, or production worker routing.
