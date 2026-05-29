# application/service

應用服務層，放跨 use case 共用的業務協調。

目前重點：
- `OrderService`：下單後撮合、持倉、ledger、market data、事件發布；symbol 有 ready worker context 時 submit 會走 matching worker execution path。
- `RiskService`：pre-trade checks、reserve、amend reserve reconciliation。
- `WalletLedgerService` / `MarginService`：帳務 posting、入出金狀態機、margin transfer。
- `WalletLedgerReplayService`：由 durable ledger replay 帳戶狀態，並提供 account/replay/delta 結構化 comparison。
- `BonusCreditService`：體驗金 grant 批次、到期 FIFO 消耗與 expiry scanner orchestration。
- `TurnoverService`：由已處理成交事件建立 turnover read model，供活動門檻與 trade/ledger 對帳。
- `AccountRiskService` / `AccountRiskSnapshotService` / `MarkPriceOracleService` / `ReconciliationService`：帳戶風險快照、snapshot 持久化、mark/index price baseline 與對帳 baseline。
- `TrialBalanceService`：由 wallet ledger postings 產生 asset/account-code trial balance。
- `ReconciliationIssueWorkflowService`：reconciliation issue claim、resolve、reopen、open queue workflow 與 workflow audit event。
- `MarketMakerProfileService` / `MarketMakerExposureService` / `MarketMakerQuoteService` / `MarketMakerHedgeStrategyService` / `MarketMakerHedgeExecutionService` / `MarketMakerHedgingService` / `MarketMakerHedgeFillService` / `MarketMakerHedgeReconciliationService`：做市商 profile、inventory exposure、quote checks、reduce-only hedge planning/execution、global execution halt、hedge risk checks、venue routing、audit event、venue fill message mapping、durable hedge decision/fill audit 與 decision-vs-fill reconciliation baseline。
- `LiquidationService` / `LiquidationScanService` / `InsuranceFundService` / `AdlRankingService` / `AdlDeleveragingPlanner` / `AdlForcedExecutionService` / `AdlQueueExecutionService`：強平掃描、decision audit、營運控制、保險基金、deterministic ADL ranking/planning、forced execution 與 queue-to-execution orchestration baseline。
- `MatchingRecoveryService`：撮合 worker startup/takeover recovery，串接 matching snapshot、command log replay 與 validation report。
- `MatchingSequencerLeaseService`：撮合 worker per-symbol lease、renew、release 與 takeover epoch baseline。
- `MatchingWorkerCommandRouter` / `MatchingWorkerExecutionService` / `MatchingWorkerLifecycleService` / `MatchingWorkerStartupListener`：撮合 worker owner/epoch guard、fenced command append、已落 log command execution、lease acquire + recovery startup、runtime startup hook、renewal/readiness baseline。
- `OutboxService`：retry、DLQ replay、manual compensation baseline。
- `MarketDataService` / `MarketDataSequenceCheckpointService` / `MarketDataRetentionService`：ticker、kline、depth delta、durable depth sequence checkpoint、reconnect backfill、durable trade tape、durable ticker latest-state、durable 1m kline 與 history retention baseline。

目前狀態：
- 這層承擔 MVP orchestration，尚未具備 production transaction boundary。
- `WalletLedgerService` 的 bonus credit 只走 ledger，不更新 `Account.crossBalance`，避免體驗金混入真實現金。
- 新增服務時要補公開方法註解，說明是否寫狀態、是否可重入、是否依賴外部系統。
