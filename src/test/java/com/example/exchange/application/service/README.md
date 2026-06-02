# test application/service

Application service tests。

目前內容：

| 測試類別 | 主要案例 |
| --- | --- |
| `OrderAccountingIntegrationTest` | 成交後帳務與持倉、風控拒單、risk tiers、max open orders、pre-trade frequency limit、重複 client order id、kill switch、批量撤單、改單、cancel-replace、cancel-on-disconnect、connection resume。 |
| `RiskSettlementServiceTest` | 單一 funding、批次 funding、強平/decision audit/營運控制、liquidation scanner batch/failure isolation、保險基金 movement、全帳戶 reconciliation。 |
| `TrialBalanceServiceTest` | Wallet ledger postings 產生 trial balance 與不平衡分類。 |
| `FinanceReportServiceTest` | Durable ledger daily/category finance report 的 reason/asset/account-code 彙總、每日 category export batch 與借貸平衡。 |
| `LedgerArchiveEligibilityServiceTest` / `LedgerArchiveManifestServiceTest` | Ledger archive/delete eligibility、manifest checksum、restore smoke、archived date-range replay validation 與 delete guard。 |
| `ReconciliationIssueWorkflowServiceTest` | 對帳 issue claim、resolve、reopen、open queue 過濾與 workflow audit event。 |
| `AdlRankingServiceTest` | ADL queue deterministic ranking 與候選排除規則。 |
| `AdlDeleveragingPlannerTest` | ADL forced deleveraging plan 分配與剩餘缺口計算。 |
| `AdlForcedExecutionServiceTest` | ADL forced execution 的減倉、ledger posting、service/durable idempotency、候選數量不足與 operator halt。 |
| `AdlQueueExecutionServiceTest` | ADL queue entry owner guard、篩選對手方候選、planning、execution、queue completion 與 partial retry remaining amount。 |
| `InsuranceFundServiceTest` | ADL queue idempotent enqueue、claim preservation、open/stuck alert report 與 insurance fund movement。 |
| `MarketDataSequenceCheckpointServiceTest` | Market-data depth sequence checkpoint 單調遞增、duplicate/out-of-order ignore、depth version recovery、depth delta backfill、durable trade tape restart read、ticker latest-state restart read 與 1m kline restart read。 |
| `MarketDataRetentionServiceTest` | Market-data depth delta、trade tape、1m kline retention cutoff 與 zero-window skip 行為。 |
| `WalletLedgerServiceTest` | 體驗金 grant、consume、expire、clawback 與 real cash 隔離。 |
| `BonusCreditServiceTest` | 體驗金批次 remaining、到期 FIFO consume、expiry scanner 與 campaign export rows。 |
| `TurnoverServiceTest` / `TurnoverReconciliationServiceTest` | 成交事件寫入 turnover fact、explicit/fallback strategy tags、match-level summary、drill-down/export 與帶 ledger-ref flag 的 trade-tape reconciliation。 |
| `MarkPriceOracleServiceTest` | oracle 設定載入、手動更新、stale quote 拒絕。 |
| `AccountRiskSnapshotServiceTest` | 單一帳戶 risk snapshot 持久化、account/open-position index 掃描。 |
| `AccountPositionConsistencyServiceTest` | Restore 後 account/open-position consistency report，涵蓋 missing account、margin shortage 與 valid case。 |
| `MatchingRecoveryServiceTest` | 撮合 worker startup/takeover recovery，從 snapshot + command log replay、assert recovered open orders 並保存 validation report。 |
| `MatchingSequencerLeaseServiceTest` | 撮合 worker lease owner 互斥、續租 checkpoint、release 與過期 takeover epoch。 |
| `OutboxServiceTest` / `OutboxDomainStateConsistencyServiceTest` | publish 失敗 retry、DLQ、replay、manual compensation、trace header 傳遞，以及 outbox row 對 domain-state projection 的 consistency report。 |
| `RpcTransactionTrackingServiceTest` | RPC transaction commandId replay、fingerprint/txHash conflict 與 unresolved outcome report。 |
| `MarginServiceTest` | 入金、成功出金、callback idempotency、出金暫停進人工覆核、manual-review claim、transfer reconciliation projection、餘額不足拒絕。 |
| `WalletLedgerReplayServiceTest` | Durable ledger replay、account comparison 與結構化 mismatch details。 |
| `AccountRiskServiceTest` | 帳戶不存在零值快照、oracle mark price 下的 equity/maintenance/risk ratio、缺 oracle 拒絕。 |
| `OperationalMetricsServiceTest` | 下單結果 counters、取消數、成交事件數、matching/DB/Redis latency 與 Kafka lag 統計。 |
| `MarketMakerHedgingServiceTest` | 做市商 exposure aggregation、kill switch、slippage rejection 與 venue routing。 |
| `MarketMakerQuoteServiceTest` | 做市商 quote kill switch、crossed quote rejection 與 quote audit event。 |
| `MarketMakerQuoteLifecycleServiceTest` / `MarketMakerQuoteReconciliationServiceTest` / `MarketMakerQuoteStateRecordTest` / `UseCaseMarketMakerQuoteOrderGatewayTest` | 做市商 quote validation 後的 stale quote cleanup、durable active quote state、per-side version metadata、quote/open-order reconciliation 與 post-only bid/ask 內部掛單 placement。 |
| `MarketMakerProfileServiceTest` | 做市商 profile/risk limit 保存、查詢與 validation。 |
| `MarketMakerHedgeFillServiceTest` | 做市商 hedge fill 保存、查詢與 validation。 |
| `MarketMakerHedgeVenueIdempotencyServiceTest` | Hedge venue submit idempotency unresolved report 與 lookup reconcile trigger。 |
| `HedgeVenueCallbackVerifierTest` | Hedge venue callback HMAC 簽章、timestamp replay window 與 secret 設定檢查。 |

注意：
- 測試使用 in-memory repository stub；行為重點是 business flow，不是 Redis/JPA。
