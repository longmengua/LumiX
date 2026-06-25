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
- `V17__market_maker_quote_state_versions.sql`: per-side bid/ask quote version and replaced order metadata for active quote restore/reconciliation.
- `V2__adl_execution_records.sql`: post-core-v1 durable ADL forced execution summary and command idempotency records.
- `V3__market_data_sequence_checkpoints.sql`: post-core-v1 durable market-data stream sequence/checksum checkpoints.
- `V4__market_data_depth_deltas.sql`: post-core-v1 durable depth deltas for reconnect backfill.
- `V5__market_data_trade_tape.sql`: post-core-v1 durable trade tape for restart-safe recent trades.
- `V6__market_data_tickers.sql`: post-core-v1 durable ticker latest-state records.
- `V7__market_data_klines.sql`: post-core-v1 durable kline records.
- `V8__hedge_venue_idempotency_records.sql`: post-core-v1 hedge venue submit idempotency records.
- `V9__polymarket_clob_command_records.sql`: post-core-v1 Polymarket CLOB command idempotency records.
- `V10__rpc_transaction_records.sql`: post-core-v1 backend-observed RPC transaction outcome tracking.
- `V11__adl_queue_entries.sql`: post-core-v1 durable ADL queue and operator claim state.
- `V12__production_query_indexes.sql`: post-core-v1 production query indexes for order projections, ledger, events, and prediction orders.
- `V23__position_lifecycle_projection.sql`: post-core-v1 live position SQL mirror schema and production query indexes.

Config:
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`

## Redis And Kafka Docs

- Redis key contract and hot-state TTL/archive policy: `docs/architecture/redis-key-schema.md`
- Live order SQL mirror/index decision: `docs/architecture/live-order-sql-mirror.md`
- Live position SQL mirror/index baseline: `docs/architecture/live-position-sql-mirror.md`, `PositionLifecycleProjection`, `PositionLifecycleProjectionJpaRepository`
- Historical data archive strategy: `docs/architecture/archive-strategy.md`
- Archive exporter skeleton: `ArchiveExporterService`, `ArchiveExporterScheduler`, `ArchiveExporterServiceTest`
- Kafka topic contract: `docs/architecture/kafka-topics.md`
- Observability baseline: `docs/operations/observability.md`
- Metrics export: `OperationalMetricsMeterBinder`, `/actuator/prometheus`
- Tracing export: Micrometer Tracing OpenTelemetry bridge, OTLP exporter, `docs/operations/tracing-dashboard.md`
- Alert backend: `OperationalAlert`, `AlertDispatchService`, `OkHttpAlertTransport`, `alerts.backend.*`
- Outbox runbook: `docs/operations/outbox-runbook.md`

## Focused Tests

Run all tests:

```bash
./mvnw test
```

Targeted tests:

```bash
./mvnw -Dtest=CommandTransactionBoundaryTest,OutboxServiceTest,OutboxDomainStateConsistencyServiceTest test
./mvnw -Dtest=InMemoryMatchingEngineTest test
./mvnw -Dtest=AccountPositionConsistencyServiceTest test
./mvnw -Dtest=MatchingLogOwnerEpochTest test
./mvnw -Dtest=MatchingWorkerCommandRouterTest test
./mvnw -Dtest=MatchingWorkerExecutionServiceTest test
./mvnw -Dtest=MatchingWorkerLifecycleServiceTest test
./mvnw -Dtest=MatchingRecoveryServiceTest test
./mvnw -Dtest=MatchingSequencerLeaseServiceTest test
./mvnw -Dtest=AdlRankingServiceTest test
./mvnw -Dtest=AdlDeleveragingPlannerTest test
./mvnw -Dtest=AdlForcedExecutionServiceTest test
./mvnw -Dtest=AdlQueueExecutionServiceTest test
./mvnw -Dtest=MarketDataSequenceCheckpointServiceTest test
./mvnw -Dtest=WalletLedgerServiceTest test
./mvnw -Dtest=BonusCreditServiceTest test
./mvnw -Dtest=TurnoverServiceTest test
./mvnw -Dtest=MarketMakerHedgingServiceTest test
./mvnw -Dtest=MarketMakerQuoteServiceTest test
./mvnw -Dtest=MarketMakerProfileServiceTest test
./mvnw -Dtest=MarketMakerHedgeFillServiceTest test
./mvnw -Dtest=MarketMakerHedgeExecutionServiceTest test
./mvnw -Dtest=LiquidateUseCaseTest test
./mvnw -Dtest=ExecuteAdlUseCaseTest test
./mvnw -Dtest=OrderAccountingIntegrationTest test
./mvnw -Dtest=RiskSettlementServiceTest test
./mvnw -Dtest=TrialBalanceServiceTest test
./mvnw -Dtest=LedgerArchiveEligibilityServiceTest,LedgerArchiveManifestServiceTest test
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

Transaction boundary coverage:
- `CommandTransactionBoundaryTest` proves successful command bodies commit, failed command bodies roll back without hidden retry, and order-place/cancel/cancel-replace/hedge persistence-style failures leave no half-state.
- `OutboxServiceTest` proves active transactions persist outbox rows first and defer external publish until `afterCommit`.
- `OutboxDomainStateConsistencyServiceTest` proves recovery reporting flags `order.lifecycle` outbox rows without matching lifecycle projection.
- `OrderAccountingIntegrationTest` covers the direct-instantiation path for place, cancel, amend, bulk cancel, cancel-on-disconnect, and cancel-replace after optional transaction boundary wiring.
- `LiquidateUseCaseTest`, `ExecuteAdlUseCaseTest`, and `MarketMakerHedgeExecutionServiceTest` prove manual liquidation, ADL forced execution, and hedge execution enter the same command boundary when configured.
- `AdlQueueExecutionServiceTest` includes restart-style partial retry coverage by reusing the same queue store across service instances and asserting only persisted remaining notional is retried.

Production worker routing coverage:
- `MatchingWorkerCommandRouterTest` proves a matching command/event append must pass the sequencer lease owner/epoch guard before the log write happens.
- `MatchingWorkerExecutionServiceTest` proves worker submit/cancel/amend/cancel-replace append fenced commands before engine execution, avoid duplicate command append, update the book, and propagate owner/epoch to matching events.
- `MatchingWorkerLifecycleServiceTest` proves configured worker startup acquires ownership, runs recovery, stores ready owner context, stays inert when disabled, rejects symbols owned by another worker, renews checkpoints, and removes readiness after renewal failure.
- `OrderAccountingIntegrationTest` includes a worker-ready cancel-replace case proving the accounting-safe cancel + replacement-submit orchestration produces fenced worker commands while preserving reserve re-hold semantics.
- `MatchingSequencerLeaseServiceTest` covers lease acquire, renew, release, takeover, missing lease, wrong owner, stale epoch, and expired lease behavior.
- `MatchingRecoveryServiceTest` includes a restore drill that rebuilds from latest snapshot plus command log and asserts recovered open orders.
- `InMemoryMatchingEngineTest` includes interleaved multi-symbol command replay validation, proving per-symbol command offsets stay independent.
- `matching-worker.*` configuration is bound by `MatchingWorkerProperties` and documented in the matching sequencer runbook.

Restore validation coverage:
- `AccountPositionConsistencyServiceTest` proves restored open positions without accounts are reported, account margin below open-position margin is flagged, and aligned restored account/position state is valid.
- `LedgerArchiveManifestServiceTest` proves archive manifests carry deterministic checksums and that the immutable delete guard requires eligibility, manifest, restore-smoke, and replay-validation checks before ledger delete can proceed.

## Agent Context Script

Use:

```bash
./shells/ai-context.sh
```

The script prints status, map entry points, TODO progress, package directories, focused tests, and changed files.
