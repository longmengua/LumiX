<!-- File purpose: Kafka topic, key, retention, and consumer-group strategy. Chinese version: ../zh-TW/kafka-topics.md. -->
# Kafka Topics

This document defines the current Kafka event contract and the production rules that should be applied when the topics are created outside the application.

中文版本：[../zh-TW/kafka-topics.md](../zh-TW/kafka-topics.md)

## Topic Matrix

| Topic | Producer | Key | Payload | Retention / Compaction |
| --- | --- | --- | --- | --- |
| `trade.executed` | `KafkaDomainEventPublisher` | `symbol` | `TradeExecuted` | Time retention; keep long enough for downstream replay. |
| `order.lifecycle` | `KafkaDomainEventPublisher` | `symbol:orderId` | `OrderLifecycleEvent` | Time retention; no compaction unless a separate projection topic exists. |
| `event.store.trade` | `KafkaEventStore` | `symbol` | `TradeExecuted` with `seq` | Long retention or DB mirror; used for replay in the MVP. |
| `funding.settled` | `KafkaDomainEventPublisher` | `symbol` | `FundingSettled` | Time retention, finance/audit consumers should archive. |
| `position.liquidated` | `KafkaDomainEventPublisher` | `symbol` | `PositionLiquidated` | Long retention, audit/archive required. |
| `domain.events` | `KafkaDomainEventPublisher` | event class name | Fallback domain event payload | Short retention; should trend toward zero use. |
| `polymarket.user.events` | `PolymarketUserWebSocketService` | wallet/order/trade derived key | `PolymarketUserWsEvent` or raw user-channel event | Long enough for user-event replay and deduplication. |

## Partition Keys

- Use `symbol` for matching, trade, funding, and liquidation events where same-symbol ordering matters.
- Use `symbol:orderId` for `order.lifecycle` so events are spread better than pure symbol keys while retaining per-order grouping.
- Use deterministic wallet/order/trade keys for Polymarket user events so duplicate user-channel events land on the same partition.
- Do not publish high-value events with random keys; random keys make replay and ordering analysis harder.

## Consumer Groups

- Use one consumer group per projection or side effect, for example `order-projection`, `market-data-projection`, `risk-audit`, and `finance-archive`.
- Consumers that update balances, positions, or user-visible order projections must be idempotent by event id, order id, match id, or lifecycle stage.
- Consumer offsets are not a substitute for business checkpoints. Projections should persist the last processed event identity.
- DLQ topics or the existing outbox DLQ workflow must be used for poison messages instead of infinite retry loops.

## Schema Versioning

- Add an explicit schema version before incompatible payload changes. For Java object serialization, introduce a wrapper field or new DTO version rather than mutating existing fields silently.
- Consumers must ignore unknown fields and tolerate nullable newly added fields.
- Incompatible changes should use a new topic name or dual-publish period.

## Production Creation

The application currently creates topics for local development. In production, create topics with infrastructure tooling and apply explicit retention, replication factor, partitions, ACLs, and monitoring.

Suggested baseline:

- Partitions: start with at least 3, increase by symbol volume and consumer parallelism.
- Replication factor: 3 in multi-broker environments.
- `min.insync.replicas`: 2 when replication factor is 3.
- Alerts: producer error rate, consumer lag, under-replicated partitions, DLQ growth, and outbox age.
