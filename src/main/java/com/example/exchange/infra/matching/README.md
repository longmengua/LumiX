<!-- File purpose: English notes for the matching engine module. Chinese version: README_ch.md. -->
# Matching Engine

This module contains the current in-memory matching implementation.

The implementation is suitable for local development and MVP validation. Before production, it should be evolved into a durable, replayable matching core with command log, event log, snapshots, offset checkpoints, and clear failover rules for each symbol sequencer.

Current files:
- `InMemoryMatchingEngine`: adapter implementing the `MatchingEngine` contract.
- `README_ch.md`: Chinese notes for this module.

Current status:
- Per-symbol operations are serialized by an in-process sequencer thread.
- LIMIT / MARKET, GTC / IOC / FOK, post-only rejection, self-match prevention, amend, cancel, and snapshot are covered by tests.
- State is still process-local and not replayable after restart.
