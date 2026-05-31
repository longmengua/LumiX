# application/service

應用服務層，放跨 use case 共用的業務協調。

目前重點：
- `OrderService`：下單後撮合、持倉、ledger、market data、事件發布；symbol 有 ready worker context 時 submit 會走 matching worker execution path。
- `CoreEventStructuredLog`：產生 `CORE_EVENT` structured log line，讓核心事件可依 uid、orderId、clientOrderId、symbol 搜尋。
- `RiskService`：pre-trade checks、uid+symbol order-entry frequency limit、reserve、amend reserve reconciliation。
- `WalletLedgerService` / `MarginService`：帳務 posting、入出金狀態機、callback 冪等、manual-review owner、transfer reconciliation projection、margin transfer。
- `WalletLedgerReplayService`：由 durable ledger replay 帳戶狀態，並提供 account/replay/delta 結構化 comparison。
- `BonusCreditService`：體驗金 grant 批次、到期 FIFO 消耗、consume eligibility gate、expiry scanner orchestration、用戶/活動 grant state report、活動 export、營運 clawback 與 campaign auto-clawback policy。
- `TurnoverService` / `TurnoverReconciliationService`：由已處理成交事件建立 turnover read model，提供 uid/symbol/strategy/market-maker/match summary、drill-down/export query 與帶 order tag / ledger ref 的 trade-tape reconciliation，供活動門檻與 trade/ledger 對帳。
- `AccountRiskService` / `AccountRiskSnapshotService` / `MarkPriceOracleService` / `ReconciliationService`：帳戶風險快照、snapshot 持久化、mark/index price baseline 與對帳 baseline。
- `TrialBalanceService`：由 wallet ledger postings 產生 asset/account-code trial balance。
- `FinanceReportService` / `FinanceExportService`：由 durable wallet ledger journal 產生 UTC daily finance report、category report 與每日 category export batch，依 reason/asset/account-code 彙總 debit/credit。
- `ReconciliationIssueWorkflowService`：reconciliation issue claim、resolve、reopen、open queue workflow 與 workflow audit event。
- `MarketMakerProfileService` / `MarketMakerExposureService` / `MarketMakerQuoteService` / `MarketMakerQuoteLifecycleService` / `MarketMakerQuoteReconciliationService` / `MarketMakerHedgeStrategyService` / `MarketMakerHedgeExecutionService` / `MarketMakerHedgingService` / `MarketMakerHedgeFillService` / `MarketMakerHedgeReconciliationService`：做市商 profile、inventory exposure、quote checks、stale quote cleanup、post-only quote order placement、durable active quote state/operator lookup、quote/open-order reconciliation、reduce-only hedge planning/execution、global execution halt、per-run route cap policy、hedge risk checks、venue routing、audit event、venue fill message mapping、durable hedge decision/fill audit 與 decision-vs-fill reconciliation baseline。
- `LiquidationService` / `LiquidationScanService` / `InsuranceFundService` / `AdlInsuranceReconciliationService` / `AdlRankingService` / `AdlDeleveragingPlanner` / `AdlForcedExecutionService` / `AdlQueueExecutionService`：強平掃描、scan batch limit、per-position failure isolation、decision audit、營運控制、保險基金、ADL stuck-claim report、insurance/ADL coverage reconciliation、recent execution report、deterministic ADL ranking/planning、forced execution 與 queue-to-execution orchestration baseline。
- `MatchingRecoveryService`：撮合 worker startup/takeover recovery，串接 matching snapshot、command log replay 與 validation report。
- `MatchingSequencerLeaseService`：撮合 worker per-symbol lease、renew、release 與 takeover epoch baseline。
- `MatchingWorkerCommandRouter` / `MatchingWorkerExecutionService` / `MatchingWorkerLifecycleService` / `MatchingWorkerStartupListener`：撮合 worker owner/epoch guard、fenced command append、已落 log command execution、lease acquire + recovery startup、runtime startup hook、renewal/readiness baseline。
- `OutboxService` / `OutboxDomainStateConsistencyService`：retry、DLQ replay、manual compensation baseline，以及 outbox row 對 domain-state projection 的 recovery consistency report。
- `RpcTransactionTrackingService`：backend-observed RPC transaction 的 durable idempotency / lifecycle tracking 與 unresolved outcome report。
- `MarketDataService` / `MarketDataSequenceCheckpointService` / `MarketDataRetentionService`：ticker、kline、depth delta、durable depth sequence checkpoint、reconnect backfill、durable trade tape、durable ticker latest-state、durable 1m kline 與 history retention baseline。

目前狀態：
- 這層承擔 MVP orchestration，尚未具備 production transaction boundary。
- `WalletLedgerService` 的 bonus credit 只走 ledger，不更新 `Account.crossBalance`，避免體驗金混入真實現金。
- 新增服務時要補公開方法註解，說明是否寫狀態、是否可重入、是否依賴外部系統。
