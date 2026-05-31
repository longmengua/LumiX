<!-- File purpose: MySQL/Redis/Kafka cross-store failure drill. Chinese version: ../zh-TW/cross-store-failure-drill.md. -->
# Cross-Store Failure Drill

Use this drill when MySQL commits but Redis hot state or Kafka/outbox publication is delayed or failed.

## Authority Order

1. MySQL domain state and durable outbox rows are authoritative after commit.
2. Kafka publication is recoverable from pending outbox rows.
3. Redis hot state is a projection and must be rebuilt from MySQL, matching logs, or lifecycle projections.

## Drill Steps

1. Stop consumers or block broker connectivity after a command commits.
2. Verify the command state in MySQL and the corresponding outbox row.
3. Run outbox/domain-state consistency:
   `GET /api/recovery/outbox/domain-state-consistency?limit=50`
4. Replay pending/dead outbox rows only after the domain state exists:
   `POST /api/recovery/outbox/dead/{outboxId}/replay`
5. Rebuild Redis hot projections from their authoritative source instead of rerunning the original command.
6. If an outbox row has no matching domain-state transition, keep it stopped and open a reconciliation issue before manual compensation.

## Pass Criteria

- No external publish occurs before database commit.
- Rollback leaves no order, ledger, hedge audit, or outbox half-state.
- Delayed Kafka publish is recovered from outbox.
- Redis is repaired from authoritative state, not from command replay.
