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
- Use `MatchingSequencerLeaseService.acquire(symbol, ownerId)` before recovery.
- Use `MatchingSequencerLeaseService.renew(...)` to extend ownership and persist observed command/event checkpoints.
- Use `MatchingSequencerLeaseService.release(...)` during planned handoff.
- Use `MatchingSequencerLeaseService.requireWritable(symbol, ownerId, epoch)` before live command writes.
- Worker startup should use `MatchingWorkerLifecycleService.startConfiguredSymbols()` to acquire configured leases, run recovery, and expose the owner/epoch context.
- Worker renewal should use `MatchingWorkerLifecycleService.renewOwnedSymbols()` to extend leases and persist observed command/event offsets.
- Worker command intake should route through `MatchingWorkerExecutionService`, which appends a lease-fenced command before applying it to the matching engine.
- Every command write must include the current `epoch`.
- Storage must reject writes from stale epochs.
- Lease renewal must stop before the worker accepts new commands if the backing store is unreachable.
- Lease TTL must be longer than the normal command processing interval and shorter than the operational failover target.

## Worker Configuration

Production worker ownership is configured by the `matching-worker` properties:

| Property | Environment variable | Meaning |
| --- | --- | --- |
| `matching-worker.enabled` | `MATCHING_WORKER_ENABLED` | Enables independent worker command intake. Default is `false`. |
| `matching-worker.owner-id` | `MATCHING_WORKER_OWNER_ID` | Unique worker owner id, usually pod / instance / process identity. |
| `matching-worker.symbols` | `MATCHING_WORKER_SYMBOLS` | Comma-separated symbols this worker should own, for example `BTCUSDT,ETHUSDT`. |
| `matching-worker.lease-ttl-ms` | `MATCHING_WORKER_LEASE_TTL_MS` | Lease TTL. Default `30000`. |
| `matching-worker.renew-interval-ms` | `MATCHING_WORKER_RENEW_INTERVAL_MS` | Lease renewal interval. Default `10000`. |
| `matching-worker.fence-legacy-routing` | `MATCHING_WORKER_FENCE_LEGACY_ROUTING` | Rejects fallback to the old in-process path for configured symbols that are not worker-ready. Default `false`. |

Do not enable worker command intake until command routing is pointed at `MatchingWorkerExecutionService` and the old REST/in-process path is fenced or halted for the same symbols.

Submit, cancel, and amend already use the worker execution path when a ready owner context exists for the symbol. Cancel-replace keeps the accounting-safe cancel + replacement-submit flow; under a ready worker context both legs are fenced worker commands.

Readiness inspection:
- `GET /api/recovery/matching-worker/contexts`
- `GET /api/recovery/matching-worker/contexts/{symbol}`

## Startup

1. Call `MatchingWorkerLifecycleService.startConfiguredSymbols()` or `startSymbol(symbol)`.
2. Acquire the symbol lease and receive a new `epoch`.
3. Call `MatchingRecoveryService.recoverSymbol(symbol)` for that symbol.
4. The recovery service loads the latest matching snapshot, replays command log entries after the checkpoint, validates the replay, and persists the recovered snapshot plus validation report.
5. Publish a readiness signal for that symbol only after recovery returns a valid report.
6. Start accepting live commands for the symbol.

When `matching-worker.enabled=true`, `MatchingWorkerStartupListener` calls `startConfiguredSymbols()` after Spring reports application readiness.

## Planned Failover

1. Stop routing new commands to the old owner.
2. Wait for in-flight commands to drain.
3. Persist a final snapshot and checkpoint.
4. Release the lease or let the new owner acquire a higher `epoch`.
5. Start the new owner with the startup recovery flow.

## Deployment Switch Sequence

1. Deploy with `MATCHING_WORKER_ENABLED=false` and confirm legacy routing still passes smoke tests.
2. Configure `MATCHING_WORKER_OWNER_ID` and `MATCHING_WORKER_SYMBOLS` for a small symbol set.
3. Enable `MATCHING_WORKER_ENABLED=true` while keeping `MATCHING_WORKER_FENCE_LEGACY_ROUTING=false`.
4. Check `GET /api/recovery/matching-worker/contexts/{symbol}` and confirm owner id, epoch, and lease expiry are present.
5. Submit, cancel, amend, and cancel-replace a test order for the symbol; verify matching command logs carry the worker owner/epoch.
6. Enable `MATCHING_WORKER_FENCE_LEGACY_ROUTING=true` for the same symbols so missing readiness rejects instead of falling back to the legacy in-process writer.
7. Monitor lease renewal, command/event offset movement, replay validation reports, and order/accounting reconciliation.
8. Roll back by setting `MATCHING_WORKER_FENCE_LEGACY_ROUTING=false`, then `MATCHING_WORKER_ENABLED=false`, and route traffic back only after confirming no active worker owns the symbol.

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

The current matching core now has durable command/event log, offset checkpoint, snapshot, validation report, recovery orchestration, lease lifecycle, service-level write guard, owner epoch audit fields, worker startup/renewal readiness lifecycle, runtime startup hook, readiness inspection endpoints, worker execution/routing for submit, cancel, amend, and cancel-replace's accounting-safe cancel + replacement-submit orchestration, plus an explicit legacy-routing fence. The remaining gap is documenting the production deployment switch sequence and running smoke verification.
