<!-- File purpose: English notes for the matching engine module. Chinese version: README_ch.md. -->
# Matching Engine

This module contains the current in-memory matching implementation.

The implementation is suitable for local development and MVP validation. Before production, it should be evolved into a durable, replayable matching core with command log, event log, snapshots, offset checkpoints, and clear failover rules for each symbol sequencer.

Current files:
- `InMemoryMatchingEngine`: adapter implementing the `MatchingEngine` contract.
- `InMemoryMatchingCommandLog`: in-memory command log baseline for replay tests.
- `InMemoryMatchingEventLog`: in-memory trade event log baseline for replay validation.
- `JpaMatchingCommandLog` / `JpaMatchingEventLog`: durable JPA log adapters used by Spring wiring.
- `JpaMatchingSnapshotStore`: durable JPA store for matching engine snapshots.
- `JpaMatchingReplayValidationReportStore`: durable JPA store for replay validation audit reports.
- `README_ch.md`: Chinese notes for this module.

Current status:
- Per-symbol operations are serialized by an in-process sequencer thread.
- LIMIT / MARKET, GTC / IOC / FOK, post-only rejection, self-match prevention, amend, cancel, and snapshot are covered by tests.
- Snapshot export/restore carries command and event offsets.
- Replay can rebuild a symbol from a snapshot checkpoint plus later command log entries.
- Replay validation compares command offset, event offset, match sequence, and aggregated book levels.
- Command/event logs, snapshots, and replay validation reports have durable schema, JPA adapter baselines, and per-symbol offset checkpoints.
- `MatchingRecoveryService` can replay startup / worker-takeover state and persist recovered snapshots plus validation reports.
- `MatchingSequencerLeaseService` provides per-symbol owner acquire/renew/release and takeover epoch baseline.
- `CANCEL_REPLACE` command replay stores a replacement order payload and replays cancel + replacement submit on the sequencer.
- `MatchingSequencerLeaseService.requireWritable(...)` rejects missing lease, wrong owner, stale epoch, and expired lease before live command writes.
- Command/event log entries can store sequencer owner id and epoch for fencing audit.
- Production worker routing still needs to call the guard.
