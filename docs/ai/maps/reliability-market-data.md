# Reliability And Market Data Map

## Outbox And DLQ

- Publisher contract: `application.event.DomainEventPublisher`
- Kafka adapter: `infra.kafka.KafkaDomainEventPublisher`
- Outbox service: `application.service.OutboxService`
- Models: `OutboxEvent`, `DlqEvent`, `OutboxEventRecord`, `DlqEventRecord`
- Repositories: `OutboxRepository`, `DlqRepository`, JPA record repositories
- Migrations:
  - `V1__reliability_baseline.sql`
  - `V5__durable_outbox_headers.sql`
- Runbook: `docs/en/outbox-runbook.md`
- Tests: `OutboxServiceTest`

Current behavior:
- Durable outbox and DLQ baseline.
- Retry backoff, max retry count, replay, manual compensation.
- Request/correlation headers retained through delayed publish.
- When a Spring transaction is active, `OutboxService.publish(...)` saves the outbox row inside the transaction and defers the external publisher call until `afterCommit`.
- Redis hot-state recovery rules are documented in `docs/en/redis-key-schema.md` and `docs/zh-TW/redis-key-schema.md`: MySQL/outbox rows are authoritative after commit; Redis projection failures should be rebuilt rather than blindly replaying the whole command.

## Event Store And Recovery

- Event store contract: `domain.repository.EventStore`
- Kafka event store: `infra.kafka.KafkaEventStore`
- Recovery API: `interfaces.web.controller.RecoveryController`
- Recovery service: `application.service.RecoveryService`
- Snapshot repository: `domain.repository.SnapshotRepository`
- Snapshot scheduler: `application.scheduler.SnapshotScheduler`

Remaining production TODO:
- Add persistence-backed transaction boundary tests proving database state and outbox rows roll back together under MySQL.
- Disaster recovery for matching, orders, accounts, and positions.

## Market Data And Push

- Controllers:
  - `interfaces.web.controller.DepthController`
  - `interfaces.web.controller.MarketDataController`
- Services:
  - `application.service.MarketDataService`
  - `application.service.PushGatewayService`
  - `application.service.MarketDataRetentionService`
- Scheduler:
  - `application.scheduler.MarketDataRetentionScheduler`
- DTOs:
  - `DepthDelta`
  - `MarketDataSequenceCheckpoint`
  - `MarketTicker`
  - `TradeTapeItem`
  - `MarketKline`
- Durable checkpoints/backfill/tape/ticker/kline: `MarketDataSequenceCheckpointService`, `MarketDataSequenceCheckpointStore`, `JpaMarketDataSequenceCheckpointStore`, `MarketDataDepthDeltaStore`, `JpaMarketDataDepthDeltaStore`, `MarketDataTradeTapeStore`, `JpaMarketDataTradeTapeStore`, `MarketDataTickerStore`, `JpaMarketDataTickerStore`, `MarketDataKlineStore`, `JpaMarketDataKlineStore`
- Checksum: `domain.util.OrderBookChecksum`
- Consumer: `interfaces.consumer.TradeEventConsumer`
- Tests:
  - `domain.util.OrderBookChecksumTest`
  - `application.service.MarketDataSequenceCheckpointServiceTest`
  - `application.service.OrderAccountingIntegrationTest`

Current behavior:
- `MarketDataService` emits monotonic depth delta versions and CRC32 checksums.
- When `MarketDataSequenceCheckpointService` is configured, depth delta version initializes from the latest durable `DEPTH_DELTA` checkpoint.
- Generated depth deltas advance the durable sequence/checksum checkpoint.
- Duplicate or out-of-order checkpoint writes are ignored deterministically.
- Depth deltas can be persisted and queried through `GET /api/market-data/{symbol}/depth-deltas?afterVersion=...` for reconnect backfill.
- Trade tape can be persisted and read back after service restart when `MarketDataTradeTapeStore` is configured.
- Ticker latest state can be persisted and read back after service restart when `MarketDataTickerStore` is configured.
- 1m klines can be persisted and read back after service restart when `MarketDataKlineStore` is configured.
- High-volume depth delta, trade tape, and 1m kline history can be purged through disabled-by-default market-data retention config; ticker latest state and sequence checkpoints are not purged by that job.

Remaining production TODO:
- Production archive export/storage for market-data history beyond local DB retention.
- Independently deployable WebSocket/SSE gateway.
