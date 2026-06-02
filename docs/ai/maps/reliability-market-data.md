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
- `OutboxDomainStateConsistencyService` and `/api/recovery/outbox/domain-state-consistency` inspect recent durable outbox rows and flag `order.lifecycle` rows without a matching lifecycle projection.
- Cross-store failure drill docs live in `docs/en/cross-store-failure-drill.md` and `docs/zh-TW/cross-store-failure-drill.md`.

## Event Store And Recovery

- Event store contract: `domain.repository.EventStore`
- Kafka event store: `infra.kafka.KafkaEventStore`
- Recovery API: `interfaces.web.controller.RecoveryController`
- Recovery service: `application.service.RecoveryService`
- Snapshot repository: `domain.repository.SnapshotRepository`
- Snapshot scheduler: `application.scheduler.SnapshotScheduler`

Remaining production TODO:
- Broaden outbox/domain-state consistency probes beyond order lifecycle as more durable projections become authoritative.
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
- Trade tape can be persisted and read back after service restart when `MarketDataTradeTapeStore` is configured; clients can replay after `tradeTs` + `matchId` through `GET /api/market-data/{symbol}/trades?afterTs=...&afterMatchId=...`.
- `GET /api/market-data/{symbol}/recovery-cursor` returns the current depth version and latest trade cursor for reconnect handoff.
- Ticker latest state can be persisted and read back after service restart when `MarketDataTickerStore` is configured.
- 1m klines can be persisted and read back after service restart when `MarketDataKlineStore` is configured.
- High-volume depth delta, trade tape, and 1m kline history can be purged through disabled-by-default market-data retention config; ticker latest state and sequence checkpoints are not purged by that job.
- `PushGatewayService.publishHeartbeat(...)` emits `gateway.heartbeat` to all active SSE/WebSocket channels with channel/timestamp payload and removes closed WebSocket sessions; `PushGatewayHeartbeatScheduler` can run it through disabled-by-default `push-gateway.heartbeat.*` config.
- `UserStreamSubscriptionAuthorizer` protects private user SSE/WebSocket streams when `api-auth.enabled=true`; admin principals may subscribe to any uid, while user principals must own the requested uid and carry `stream:read`, `user:stream`, or `user:read` scope. WebSocket handshakes also accept `apiKey`, `access_token`, or `token` query parameters for browser clients.
- `MarketDataStreamRateLimiter` applies per-client fixed-window limits to market/user SSE subscriptions and WebSocket handshakes through `push-gateway.rate-limit.*`.
- `docs/en/market-data-gateway-scaling.md` and `docs/zh-TW/market-data-gateway-scaling.md` document the independently deployable gateway role, broadcast fanout requirement for horizontally scaled instances, load-balancer draining, shared rate-limit options, heartbeat policy, reconnect replay flow, readiness, and rollback.

Remaining production TODO:
- Production archive export/storage for market-data history beyond local DB retention.
- Execute the WebSocket/SSE gateway split in production infrastructure using the documented horizontal-scaling runbook.
