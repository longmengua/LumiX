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
- `V7__matching_replay_logs.sql`: durable matching command/event log, offset checkpoint, engine snapshot, and replay validation report baseline.
- `V8__matching_sequencer_leases.sql`: durable matching sequencer lease / epoch baseline.
- `V9__matching_cancel_replace_commands.sql`: matching cancel-replace replacement order payload.
- `V10__matching_owner_epoch_logs.sql`: matching command/event log owner epoch audit fields.
- `V11__turnover_records.sql`: durable turnover read model with user, symbol, strategy, market-maker, order, match, and sequence dimensions.
- `V12__bonus_credit_grants.sql`: durable bonus credit grant batches with remaining amount and expiry status.
- `V13__reconciliation_issue_workflow.sql`: reconciliation issue status, owner, and resolved timestamp columns.
- `V14__market_maker_profiles.sql`: durable market-maker profile and per-symbol risk limits.
- `V15__hedge_decision_audits.sql`: durable hedge decision audit trail by market-maker, symbol, ref id, and venue order id.
- `V16__hedge_fills.sql`: durable hedge fill audit trail by venue order/fill id, ref id, quantity, price, and fee.

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
./mvnw -Dtest=MatchingLogOwnerEpochTest test
./mvnw -Dtest=MatchingRecoveryServiceTest test
./mvnw -Dtest=MatchingSequencerLeaseServiceTest test
./mvnw -Dtest=AdlRankingServiceTest test
./mvnw -Dtest=AdlDeleveragingPlannerTest test
./mvnw -Dtest=WalletLedgerServiceTest test
./mvnw -Dtest=BonusCreditServiceTest test
./mvnw -Dtest=TurnoverServiceTest test
./mvnw -Dtest=MarketMakerHedgingServiceTest test
./mvnw -Dtest=MarketMakerQuoteServiceTest test
./mvnw -Dtest=MarketMakerProfileServiceTest test
./mvnw -Dtest=MarketMakerHedgeFillServiceTest test
./mvnw -Dtest=OrderAccountingIntegrationTest test
./mvnw -Dtest=RiskSettlementServiceTest test
./mvnw -Dtest=TrialBalanceServiceTest test
./mvnw -Dtest=OutboxServiceTest test
./mvnw -Dtest=MarginServiceTest test
./mvnw -Dtest=AccountRiskServiceTest test
./mvnw -Dtest=WalletLedgerReplayServiceTest test
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
