# Persistence And Tests Map

## Flyway

Flyway is the schema owner. Hibernate validates schema and must not use `ddl-auto=update`.

Migrations:
- `V1__reliability_baseline.sql`: outbox and DLQ baseline.
- `V2__order_lifecycle_projection.sql`: lifecycle event log and latest-state projection.
- `V3__wallet_ledger_journal.sql`: double-entry wallet ledger journal.
- `V4__reconciliation_reports.sql`: persisted reconciliation report and issue detail.
- `V5__durable_outbox_headers.sql`: durable outbox headers and compensation compatibility.
- `V6__account_risk_snapshots.sql`: account risk snapshot records.

Config:
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`

## Redis And Kafka Docs

- Redis key contract: `docs/en/redis-key-schema.md`
- Kafka topic contract: `docs/en/kafka-topics.md`
- Observability baseline: `docs/en/observability.md`
- Outbox runbook: `docs/en/outbox-runbook.md`

## Focused Tests

Run all tests:

```bash
./mvnw test
```

Targeted tests:

```bash
./mvnw -Dtest=InMemoryMatchingEngineTest test
./mvnw -Dtest=OrderAccountingIntegrationTest test
./mvnw -Dtest=RiskSettlementServiceTest test
./mvnw -Dtest=OutboxServiceTest test
./mvnw -Dtest=MarginServiceTest test
./mvnw -Dtest=AccountRiskServiceTest test
./mvnw -Dtest=ApiAuthenticationInterceptorTest test
```

Test ownership:
- Matching rules: `src/test/java/com/example/exchange/infra/matching`
- Application flows: `src/test/java/com/example/exchange/application/service`
- Utilities: `src/test/java/com/example/exchange/domain/util`
- Web security/interceptors: `src/test/java/com/example/exchange/interfaces/web`

## Agent Context Script

Use:

```bash
./shells/ai-context.sh
```

The script prints status, map entry points, TODO progress, package directories, focused tests, and changed files.
