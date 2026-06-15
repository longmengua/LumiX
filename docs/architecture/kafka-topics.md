<!-- 檔案用途：Kafka topic、partition key、retention 與 consumer group 策略。 -->
# Kafka Topics

這份文件定義目前 Kafka event contract，以及正式環境用基礎設施建立 topic 時應套用的規則。


## Topic Matrix

| Topic | Producer | Key | Payload | Retention / Compaction |
| --- | --- | --- | --- | --- |
| `trade.executed` | `KafkaDomainEventPublisher` | `symbol` | `TradeExecuted` | 時間型 retention；保留足夠時間供下游 replay。 |
| `order.lifecycle` | `KafkaDomainEventPublisher` | `symbol:orderId` | `OrderLifecycleEvent` | 時間型 retention；除非另有 projection topic，否則不要 compaction。 |
| `event.store.trade` | `KafkaEventStore` | `symbol` | 帶 `seq` 的 `TradeExecuted` | 長 retention 或 DB mirror；MVP 用於 replay。 |
| `funding.settled` | `KafkaDomainEventPublisher` | `symbol` | `FundingSettled` | 時間型 retention，finance/audit consumer 需要 archive。 |
| `position.liquidated` | `KafkaDomainEventPublisher` | `symbol` | `PositionLiquidated` | 長 retention，需要 audit/archive。 |
| `domain.events` | `KafkaDomainEventPublisher` | event class name | fallback domain event payload | 短 retention；正式路線應逐步降到零使用。 |
| `polymarket.user.events` | `PolymarketUserWebSocketService` | wallet/order/trade derived key | `PolymarketUserWsEvent` 或 raw user-channel event | 保留足夠時間供 user-event replay 與 deduplication。 |

## Partition Keys

- matching、trade、funding、liquidation 這類同 symbol 順序重要的事件使用 `symbol`。
- `order.lifecycle` 使用 `symbol:orderId`，比純 symbol 更能分散，同時保留單筆 order grouping。
- Polymarket user events 使用 deterministic wallet/order/trade key，讓重複 user-channel events 進同一個 partition。
- 高價值事件不要使用 random key；random key 會讓 replay 與順序分析更困難。

## Consumer Groups

- 每個 projection 或 side effect 使用獨立 consumer group，例如 `order-projection`、`market-data-projection`、`risk-audit`、`finance-archive`。
- 會更新 balance、position 或使用者可見 order projection 的 consumer，必須依 event id、order id、match id 或 lifecycle stage 做冪等。
- consumer offset 不是 business checkpoint。Projection 應保存最後處理的 event identity。
- poison message 應進 DLQ topic 或既有 outbox DLQ 流程，不要無限 retry。

## Schema Versioning

- incompatible payload change 前要加入明確 schema version。若目前用 Java object serialization，應加 wrapper field 或新 DTO version，不要靜默改既有欄位語意。
- Consumer 必須忽略未知欄位，並容忍新欄位為 null。
- incompatible change 應使用新 topic name 或 dual-publish 過渡期。

## Production Creation

目前 application 會為本機開發建立 topic。正式環境應改由基礎設施工具建立 topic，並明確設定 retention、replication factor、partitions、ACL 與監控。

建議基線：

- Partitions：至少 3 起，依 symbol 流量與 consumer parallelism 增加。
- Replication factor：多 broker 環境使用 3。
- `min.insync.replicas`：replication factor 為 3 時設 2。
- Alerts：producer error rate、consumer lag、under-replicated partitions、DLQ growth、outbox age。
