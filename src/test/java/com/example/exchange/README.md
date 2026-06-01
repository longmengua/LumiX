# test com.example.exchange

測試 package root。

目前測試範圍：
- `ExchangeApplicationTests`：Spring context smoke test。
- `application/service/`：帳務、風控、outbox、metrics、risk snapshot。
- `infra/matching/`：in-memory matching engine。
- `domain/util/`：checksum、log sanitizer。
- `interfaces/web/`：security、interceptor。

測試類別索引：

| 測試類別 | 主要驗證 |
| --- | --- |
| `ExchangeApplicationTests` | application class 能載入，避免啟動入口破壞 Spring 掃描。 |
| `AccountRiskServiceTest` | risk snapshot 的 equity、PNL、maintenance margin、risk ratio。 |
| `MarginServiceTest` | deposit/withdraw transfer state machine、callback idempotency、manual-review owner、transfer reconciliation projection 與 ledger side effect。 |
| `OperationalMetricsServiceTest` | 下單結果、撤單、成交事件、延遲統計。 |
| `OrderAccountingIntegrationTest` | 下單到撮合、帳務、position、market data、lifecycle event、pre-trade frequency limit 的整合流程。 |
| `OutboxServiceTest` / `OutboxDomainStateConsistencyServiceTest` | outbox retry、DLQ、replay、manual compensation、trace headers，以及 outbox/domain-state consistency report。 |
| `RpcTransactionTrackingServiceTest` | RPC transaction commandId replay、fingerprint/txHash conflict 與 unresolved outcome report。 |
| `PolymarketApprovalServiceTest` | RPC approval read cache、owner clear 與 TTL refresh。 |
| `PolymarketOrderServiceTest` | CLOB place `clientRequestId` idempotency、payload conflict 與 uncertain local-order retry blocking。 |
| `PolymarketOrderTrackingServiceTest` | CLOB cancel local idempotency replay、durable commandId replay/conflict、第一次 cancel 成功 marker、exception/5xx uncertain outcome、uncertain cancel reconcile resolution、sync/reconcile unchanged no-op replay、terminal status/matched-size downgrade guard。 |
| `PolymarketSessionServiceTest` | Session signer revoke-all 覆蓋 pending/active、過期使用拒絕與 EXPIRED 標記。 |
| `PolymarketUserEventServiceTest` | User-channel callback `eventKey` replay、duplicate-key race no-op 與 order side effect 去重。 |
| `RiskSettlementServiceTest` | funding、liquidation、liquidation scanner batch/failure isolation、insurance fund movement、account reconciliation。 |
| `InsuranceFundServiceTest` | ADL queue enqueue by liquidation id、duplicate replay、operator claim preservation、ADL alert report、insurance fund movement。 |
| `AdlInsuranceReconciliationServiceTest` | ADL queue shortfall 與 liquidated-position insurance/ADL coverage 對帳。 |
| `AdlForcedExecutionServiceTest` | ADL forced execution、ledger postings、idempotency、operator halt 與 recent execution report。 |
| `AdlQueueExecutionServiceTest` | ADL queue 到 ranking/planning/execution 的 orchestration，含 restart-style partial retry。 |
| `IdempotentHedgeVenueAdapterTest` | Hedge venue submit refId idempotency、payload conflict、pending/uncertain timeout duplicate blocking。 |
| `RealHedgeVenueAdapterTest` | Real hedge venue skeleton 的 disabled safety 與 signed request contract。 |
| `MarketDataSequenceCheckpointServiceTest` | Market-data depth sequence checkpoint、backfill、durable trade tape、ticker latest-state 與 1m kline baseline。 |
| `MarketDataRetentionServiceTest` | Market-data high-volume history retention baseline。 |
| `AccountPositionConsistencyServiceTest` | Restore 後檢查 account/open-position consistency issue 與 valid report。 |
| `ExecuteAdlUseCaseTest` | ADL forced execution 入口的 transaction boundary commit/rollback。 |
| `CommandTransactionBoundaryTest` / `OrderCommandTransactionBoundaryTest` | command body 與 place/cancel/amend/cancel-replace 指令入口失敗時 rollback command transaction。 |
| `OrderBookChecksumTest` | order book checksum 的 BigDecimal scale normalization。 |
| `SensitiveLogSanitizerTest` | query、JSON、Authorization header、known sensitive header 遮罩。 |
| `InMemoryMatchingEngineTest` | FIFO、post-only、自成交拒絕、FOK/IOC、市價單流動性不足、snapshot/replay、multi-symbol replay validation。 |
| `ApiAuthenticationInterceptorTest` | API auth 開關、401、403、role/scope 授權、principal 寫入。 |
| `ProtectedApiSecurityInterceptorTest` | IP allowlist 與 per-IP rate limit。 |
| `RequestLoggingInterceptorTest` | request/correlation id、response headers、MDC lifecycle。 |
| `ApiKeyAuthenticatorTest` | API key hash 驗證與 roles/scopes 解析。 |
| `IpAllowlistTest` | 精確 IP、萬用字元、IPv4 CIDR、拒絕分支。 |
| `JwtAuthenticatorTest` | HS256 JWT 簽章、exp、roles/scopes。 |

目前狀態：
- 測試偏 focused unit/integration style，避免依賴完整外部服務。
- 大多數 repository 在測試內使用 in-memory stub。
