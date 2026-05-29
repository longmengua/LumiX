<!-- File purpose: Redis key schema, ownership, TTL, and migration notes. Chinese version: ../zh-TW/redis-key-schema.md. -->
# Redis Key Schema

This document records the current Redis keys used by the low-latency exchange repositories. Production deployments should treat these names as a versioned contract.

中文版本：[../zh-TW/redis-key-schema.md](../zh-TW/redis-key-schema.md)

## Current Keys

| Key pattern | Type | Owner | Value | TTL |
| --- | --- | --- | --- | --- |
| `acc:{uid}` | String | `RedisAccountRepository` | `Account` object | None |
| `acc:index` | Set | `RedisAccountRepository` | Known account uids for reconciliation scans | None |
| `pos:{uid}` | Hash | `RedisPositionRepository` | field `symbolCode` -> `Position` object | None |
| `pos:open:index` | Set | `RedisPositionRepository` | members `{uid}:{symbolCode}` for non-zero positions | None |
| `order:{uuid}` | String | `RedisOrderRepository` | `Order` object | None |
| `ord:list:{uid}` | List | `RedisOrderRepository` | Ordered order ids | None |
| `ord:set:{uid}` | Set | `RedisOrderRepository` | Dedup set for order ids | None |
| `snap:{uid}` | String | `RedisSnapshotRepository` | Latest `Snapshot` object | None |
| `wallet:ledger:{uuid}` | String | `RedisWalletLedgerRepository` | `WalletLedgerEntry` object | None |
| `wallet:ledger:uid:{uid}` | List | `RedisWalletLedgerRepository` | Ledger entry ids by user | None |
| `wallet:ledger:ref:{refId}` | List | `RedisWalletLedgerRepository` | Ledger entry ids by reference | None |
| `outbox:index` | List | `RedisOutboxRepository` | Outbox event ids | None |
| `outbox:event:{uuid}` | String | `RedisOutboxRepository` | `OutboxEvent` object | None |
| `dlq:index` | List | `RedisDlqRepository` | DLQ event ids, newest first | None |
| `dlq:event:{uuid}` | String | `RedisDlqRepository` | `DlqEvent` object | None |
| `idempotency:{key}` | String | `RedisIdempotencyRepository` | Marker value `1` | Request-defined expiry when available |

## Production Rules

- Use `REDIS_KEY_PREFIX` before production traffic, for example `mh:v1`, which yields keys such as `mh:v1:acc:{uid}`. The default is empty for backward compatibility.
- Treat account, position, order, ledger, outbox, DLQ, and snapshot keys as durable operational state. They should not expire automatically unless an archive and replay path already exists.
- Idempotency keys are the exception: they should always have a bounded TTL for command deduplication windows.
- Open-position scans should read `pos:open:index`. Do not use `KEYS pos:*` in production-size environments except for one-time migration tooling.
- Keep list and set indexes consistent with object keys. If a repository deletes or archives `order:{uuid}`, it must also clean `ord:list:{uid}` and `ord:set:{uid}`.
- For schema changes, introduce a new namespace version (`mh:v2:*`) or dual-read/dual-write migration. Do not silently change serialized object shape under the same key pattern.

## Final TTL And Archive Policy

| Key family | Production TTL | Archive / deletion rule | Authoritative rebuild source |
| --- | --- | --- | --- |
| `acc:{uid}`, `acc:index` | No automatic TTL | Keep while account is active. Remove only after account closure, finance export, and ledger replay verification. | Wallet ledger journal, account snapshots, reconciliation reports |
| `pos:{uid}`, `pos:open:index` | No automatic TTL | Keep open positions indefinitely. Closed zero-quantity positions may be removed from `pos:{uid}` after order/trade archive and risk snapshot export. | Matching event log, order lifecycle projection, ledger replay, risk snapshots |
| `order:{uuid}` | No TTL for open orders; archive closed orders after the historical-order retention window. | Archive terminal orders before deleting object keys; always remove corresponding ids from `ord:list:{uid}` and `ord:set:{uid}` in the same maintenance job. | Order lifecycle events/projection and matching command/event logs |
| `ord:list:{uid}`, `ord:set:{uid}` | No automatic TTL | Trim or rebuild only after terminal order archive; indexes must not reference deleted `order:{uuid}` keys. | Order lifecycle projection by `uid` |
| `snap:{uid}` | No automatic TTL | Keep latest snapshot; replace atomically after successful snapshot generation. Historical snapshots belong in the SQL `snapshots` table or archive storage. | SQL snapshots table and replayable aggregate logs |
| `wallet:ledger:*` | No Redis TTL until Redis ledger is fully replaced by SQL reads. | Once SQL ledger reads are authoritative, Redis ledger keys become cache and may use a bounded cache TTL such as 24h; finance/audit retention must stay in MySQL/archive. | Wallet ledger journal tables |
| `outbox:*`, `dlq:*` | No production TTL while legacy Redis adapters are active. | Prefer MySQL outbox/DLQ. If Redis legacy keys exist, archive payloads and delete only terminal published/DLQ records after compensation review. | MySQL outbox/DLQ tables and Kafka/archive consumers |
| `idempotency:{key}` | Required bounded TTL from the command dedupe window. | Let Redis expire naturally; never archive as business evidence. Durable command/outcome records must carry audit history. | Durable command logs, idempotency stores, ledger/outbox state |

Maintenance jobs must delete object keys before or together with secondary indexes only when a rebuild source is available. If index cleanup fails, the repair action is to rebuild the index from the authoritative source, not to recreate archived objects in Redis.

## Transaction Boundary And Recovery Rules

- MySQL is the authoritative store for command results that have a durable schema. Redis hot state is a serving cache or fast index unless a specific repository still lacks a MySQL replacement.
- A command transaction must commit database state and outbox rows before external publish. If the database transaction rolls back, Redis updates and Kafka publish must be considered invalid and repaired from the authoritative database or replay log.
- If MySQL commits but a Redis write fails, do not retry the whole command blindly. Retry or rebuild only the Redis hot-state projection from durable orders, positions, ledger journals, matching logs, or outbox events.
- If Redis succeeds but MySQL rolls back, treat the Redis value as stale. Recovery tooling must be able to overwrite account, position, order, open-position index, and order indexes from durable storage.
- Idempotency keys protect command retry windows, but they are not a substitute for replay. If a command outcome is uncertain, operators should compare durable command/outbox/ledger state before accepting a duplicate retry.
- Outbox/DLQ Redis keys are legacy hot-state implementations when the durable JPA repository is not active. Production should prefer the MySQL outbox repository so outbox rows participate in the same transaction boundary.

## ADL Hot-State Repair Rules

- ADL forced execution must treat `adl_execution_records`, wallet ledger journal rows, and persisted position/account state as authoritative after commit.
- ADL queue state in Redis or memory is operational hot state. If ADL execution commits but queue completion fails, operators should not re-run the same command blindly; they should read the durable execution record by `commandId` and remove or rebuild only the queue projection for the completed `liquidationId`.
- If ADL execution rolls back after an operator claimed a queue item, keep or restore the claim state and retry with the same `commandId` only after confirming no durable execution record exists.
- If account, position, or open-position Redis indexes drift after ADL execution, rebuild them from durable position/account records and ledger replay; do not compensate by posting another ADL ledger entry.
- If a queue item remains partially uncovered, keep it queued with the remaining shortfall and a new command id for the next execution attempt; do not reuse a completed command id for a new amount.

## Migration Backlog

- Enable `REDIS_KEY_PREFIX` in each environment and plan a one-time migration for existing un-prefixed data.
- Keep the maintained account and open-position indexes healthy; add repair tooling that can rebuild them from durable storage.
- Implement the maintenance jobs described in the final TTL/archive policy for historical orders, wallet ledger entries, outbox events, DLQ events, and snapshots.
- Move long-lived financial records to the production database ledger schema, keeping Redis as a serving cache or hot-state store.
