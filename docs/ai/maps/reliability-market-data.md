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

## Event Store And Recovery

- Event store contract: `domain.repository.EventStore`
- Kafka event store: `infra.kafka.KafkaEventStore`
- Recovery API: `interfaces.web.controller.RecoveryController`
- Recovery service: `application.service.RecoveryService`
- Snapshot repository: `domain.repository.SnapshotRepository`
- Snapshot scheduler: `application.scheduler.SnapshotScheduler`

Remaining production TODO:
- Extend explicit transaction boundaries beyond the place-order path to cancel/amend/cancel-replace, liquidation, ADL execution, hedge execution, and Redis hot-state recovery.
- Disaster recovery for matching, orders, accounts, and positions.

## Market Data And Push

- Controllers:
  - `interfaces.web.controller.DepthController`
  - `interfaces.web.controller.MarketDataController`
- Services:
  - `application.service.MarketDataService`
  - `application.service.PushGatewayService`
- DTOs:
  - `DepthDelta`
  - `MarketTicker`
  - `TradeTapeItem`
  - `MarketKline`
- Checksum: `domain.util.OrderBookChecksum`
- Consumer: `interfaces.consumer.TradeEventConsumer`
- Tests:
  - `domain.util.OrderBookChecksumTest`
  - `application.service.OrderAccountingIntegrationTest`

Remaining production TODO:
- Durable sequence checkpoints and reconnect backfill.
- Ticker/kline/trade tape persistence.
- Independently deployable WebSocket/SSE gateway.
