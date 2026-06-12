# Test Priority Map

This repository keeps only P0 executable tests by default. Lower-priority tests are documented here so future agents can decide whether to restore a specific coverage area.

## Priority Rules

- P0: must keep. Covers application boot, customer auth, API security, core matching, order accounting, margin/risk, market-maker quote lifecycle, exchange WebSocket, public market API, and prod-facing exchange UI smoke.
- P1: high-value production integrity coverage, but not part of the minimal always-kept suite.
- P2: operator/admin workflows and recovery coverage.
- P3: reporting, observability, archival, reconciliation, and diagnostics.
- P4: external venue and Polymarket integration coverage.
- P5: convenience, legacy, or broad UI smoke coverage outside the prod-facing customer path.

## Kept P0 Tests

- `src/test/java/com/example/exchange/ExchangeApplicationTests.java`: P0 boot smoke.
- `src/test/java/com/example/exchange/application/service/AccountRiskServiceTest.java`: P0 account risk checks.
- `src/test/java/com/example/exchange/application/service/AuthServiceTest.java`: P0 customer registration, login, logout, and email-verification auth contract.
- `src/test/java/com/example/exchange/application/service/CommandTransactionBoundaryTest.java`: P0 command rollback boundary.
- `src/test/java/com/example/exchange/application/service/HumanVerificationServiceTest.java`: P0 registration abuse-control fail-closed behavior.
- `src/test/java/com/example/exchange/application/service/MarginServiceTest.java`: P0 margin/account read model.
- `src/test/java/com/example/exchange/application/service/MarketMakerAutoQuoteServiceTest.java`: P0 automatic quote runner baseline.
- `src/test/java/com/example/exchange/application/service/MarketMakerQuoteLifecycleServiceTest.java`: P0 market-maker quote state lifecycle.
- `src/test/java/com/example/exchange/application/service/MarketMakerQuoteServiceTest.java`: P0 quote command validation and kill switch.
- `src/test/java/com/example/exchange/application/service/OrderAccountingIntegrationTest.java`: P0 order placement, accounting, risk, cancel/amend/cancel-on-disconnect integration.
- `src/test/java/com/example/exchange/domain/util/SensitiveLogSanitizerTest.java`: P0 secret/log sanitization.
- `src/test/java/com/example/exchange/infra/matching/InMemoryMatchingEngineTest.java`: P0 matching, FIFO, replay, and snapshot behavior.
- `src/test/java/com/example/exchange/interfaces/web/WebSocketPushConfigTest.java`: P0 exchange WebSocket user subscription behavior.
- `src/test/java/com/example/exchange/interfaces/web/controller/MarketControllerTest.java`: P0 public market API does not expose admin-only fields.
- `src/test/java/com/example/exchange/interfaces/web/interceptor/ApiAuthenticationInterceptorTest.java`: P0 API auth enforcement.
- `src/test/java/com/example/exchange/interfaces/web/interceptor/ProtectedApiSecurityInterceptorTest.java`: P0 protected API allowlist/rate-limit enforcement.
- `src/test/java/com/example/exchange/interfaces/web/security/ApiKeyAuthenticatorTest.java`: P0 API key authentication.
- `src/test/java/com/example/exchange/interfaces/web/security/JwtAuthenticatorTest.java`: P0 JWT authentication.
- `src/test/java/com/example/exchange/interfaces/web/security/ProtectedApiClassifierTest.java`: P0 endpoint security classification.
- `tests/e2e/static-admin-pages.spec.js`: P0 retains only `exchange console renders client trading workflow without admin funding controls`.

## Removed P1 Tests

- `AccountPositionConsistencyServiceTest`: P1 account/position consistency checks.
- `AccountRiskSnapshotServiceTest`: P1 risk snapshot publication.
- `MarketDataSequenceCheckpointServiceTest`: P1 market-data persistence/recovery cursor coverage.
- `MatchingBookRecoveryServiceTest`: P1 matching book recovery.
- `MatchingRecoveryServiceTest`: P1 matching replay recovery.
- `MatchingSequencerLeaseServiceTest`: P1 matching sequencer lease behavior.
- `MatchingWorkerCommandRouterTest`: P1 worker routing.
- `MatchingWorkerExecutionServiceTest`: P1 worker fenced command execution.
- `MatchingWorkerLifecycleServiceTest`: P1 worker lifecycle/readiness.
- `OrderCommandTransactionBoundaryTest`: P1 use-case transaction wrappers.
- `OrderLifecycleProjectionServiceTest`: P1 lifecycle projection and rebuild.
- `OutboxServiceTest`: P1 outbox persistence.
- `WalletLedgerReplayServiceTest`: P1 wallet ledger replay and comparison.
- `WalletLedgerServiceTest`: P1 wallet ledger posting behavior.

## Removed P2 Tests

- `AdlDeleveragingPlannerTest`: P2 ADL planning.
- `AdlForcedExecutionServiceTest`: P2 ADL forced execution.
- `AdlInsuranceReconciliationServiceTest`: P2 ADL insurance reconciliation.
- `AdlQueueExecutionServiceTest`: P2 ADL queue execution.
- `AdlRankingServiceTest`: P2 ADL ranking.
- `ExecuteAdlUseCaseTest`: P2 ADL use case transaction entry.
- `InsuranceFundServiceTest`: P2 insurance fund workflow.
- `LiquidateUseCaseTest`: P2 liquidation transaction entry.
- `RiskSettlementServiceTest`: P2 settlement risk workflow.
- `AdminDlqControllerTest`: P2 admin DLQ API.
- `AdminMarketConfigControllerTest`: P2 admin market config API.
- `AdminRiskParametersControllerTest`: P2 admin risk parameters API.
- `AdminTestFundsControllerTest`: P2 admin test-funds API.
- `MarginControllerAccountResponseTest`: P2 account response formatting.
- Admin E2E cases in `tests/e2e/static-admin-pages.spec.js`: P2/P5 operator UI smoke coverage removed from execution.

## Removed P3 Tests

- `AlertDispatchServiceTest`: P3 alert dispatch.
- `ArchiveExporterServiceTest`: P3 archive export planning.
- `BonusCreditServiceTest`: P3 bonus credit campaign operations.
- `CoreEventStructuredLogTest`: P3 structured log formatting.
- `FinanceReportServiceTest`: P3 finance reporting.
- `LedgerArchiveEligibilityServiceTest`: P3 ledger archive eligibility.
- `LedgerArchiveManifestServiceTest`: P3 ledger archive manifest.
- `MarketDataRetentionServiceTest`: P3 market-data retention cleanup.
- `OperationalMetricsServiceTest`: P3 operational metrics.
- `OperationalMetricsMeterBinderTest`: P3 Micrometer binding.
- `OutboxDomainStateConsistencyServiceTest`: P3 outbox/domain consistency inspection.
- `PushGatewayServiceTest`: P3 gateway heartbeat/runtime behavior.
- `ReconciliationIssueWorkflowServiceTest`: P3 reconciliation issue workflow.
- `ReconciliationReportServiceTest`: P3 reconciliation report persistence.
- `RpcTransactionTrackingServiceTest`: P3 RPC transaction tracking.
- `TrialBalanceServiceTest`: P3 trial balance reporting.
- `TurnoverReconciliationServiceTest`: P3 turnover reconciliation.
- `TurnoverServiceTest`: P3 turnover reporting.

## Removed P4 Tests

- `HedgeVenueCallbackVerifierTest`: P4 hedge venue callback verification.
- `IdempotentHedgeVenueAdapterTest`: P4 hedge venue idempotency adapter.
- `RealHedgeVenueAdapterTest`: P4 real hedge venue HTTP adapter.
- `RetryingHedgeVenueAdapterTest`: P4 retrying hedge venue adapter.
- `ThrottlingHedgeVenueAdapterTest`: P4 throttling hedge venue adapter.
- `MarketMakerHedgeExecutionServiceTest`: P4 market-maker hedge execution.
- `MarketMakerHedgeFillServiceTest`: P4 market-maker hedge fills.
- `MarketMakerHedgeReconciliationServiceTest`: P4 hedge reconciliation.
- `MarketMakerHedgeStrategyServiceTest`: P4 hedge strategy planning.
- `MarketMakerHedgeVenueIdempotencyServiceTest`: P4 hedge venue idempotency service.
- `MarketMakerHedgingServiceTest`: P4 hedging exposure aggregation.
- `MarketMakerProfileServiceTest`: P4 market-maker profile persistence.
- `MarketMakerQuoteReconciliationServiceTest`: P4 quote/open-order reconciliation.
- `UseCaseMarketMakerQuoteOrderGatewayTest`: P4 quote order gateway.
- `MarketMakerQuoteRateLimiterTest`: P4 market-maker quote rate limit.
- `MarketMakerHedgeExecutionRateLimiterTest`: P4 market-maker hedge rate limit.
- `MarketMakerEndpointAuditLoggerTest`: P4 market-maker endpoint audit details.
- `MarketMakerQuoteStateRecordTest`: P4 quote state record entity.
- `PolymarketApprovalServiceTest`: P4 Polymarket approval cache.
- `PolymarketOrderServiceTest`: P4 Polymarket order service.
- `PolymarketOrderStateMachineTest`: P4 Polymarket order lifecycle state machine.
- `PolymarketOrderTrackingServiceTest`: P4 Polymarket order tracking.
- `PolymarketResponseSchemaValidatorTest`: P4 Polymarket response validation.
- `PolymarketSessionServiceTest`: P4 Polymarket session service.
- `PolymarketUserEventServiceTest`: P4 Polymarket user event mapping.
- `PolymarketUserWebSocketServiceTest`: P4 Polymarket user WebSocket worker.

## Removed P5 Tests

- `InMemoryAdlQueueStoreTest`: P5 in-memory ADL store convenience coverage.
- `OrderBookChecksumTest`: P5 checksum helper coverage.
- `WalletLedgerHashTest`: P5 ledger hash helper coverage.
- `MatchingLogOwnerEpochTest`: P5 matching log owner/epoch persistence detail.
- `RequestLoggingInterceptorTest`: P5 request logging convenience behavior.
- `IpAllowlistTest`: P5 standalone allowlist helper behavior now represented by protected API security tests.
- `MarketDataStreamRateLimiterTest`: P5 standalone stream rate limiter.
- `UserStreamSubscriptionAuthorizerTest`: P5 standalone user-stream authorizer.
- Legacy `trading console loads the core workflow controls` E2E case: P5 legacy Polymarket static page smoke.
