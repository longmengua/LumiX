# domain/model/dto

跨層傳遞的資料模型與讀模型。

目前重點：
- Market data：`DepthDelta`、`MarketTicker`、`MarketKline`、`TradeTapeItem`。
- 風控/帳務：`AccountRiskSnapshot`、`MarkPriceSnapshot`、`FundingSettlementResult`、`LiquidationResult`、`AdlInsuranceReconciliationReport`、`AdlInsuranceReconciliationIssue`、`BonusCreditGrant`、`BonusCreditReport`、`BonusCreditCampaignReport`、`BonusCreditCampaignExport`、`TurnoverRecord`、`TurnoverSummary`、`TurnoverExportReport`、`TurnoverReconciliationReport`；`TurnoverReconciliationIssue` 會攜帶 order tag 與 ledger-ref presence 方便營運對帳。
- Snapshot read model：account risk snapshots、reconciliation reports、wallet ledger replay。
- Finance/recovery read model：`TrialBalanceReport`、`TrialBalanceLine`、`FinanceDailyReport`、`FinanceDailyReportLine`、`FinanceCategoryExportBatch`、`LedgerArchiveRestoreSmokeReport`、`LedgerArchiveReplayValidationReport`、`LedgerReplayComparisonReport`、`LedgerReplayComparisonIssue`、`OutboxDomainStateConsistencyReport`、`OutboxDomainStateConsistencyIssue`。
- Market maker / hedging：`MarketMakerProfile`、`MarketMakerRiskLimit`、`MarketMakerExposure`、`MarketMakerQuoteCommand`、`MarketMakerQuoteDecision`、`MarketMakerQuoteLifecycleReport`、`MarketMakerQuoteState`（含 per-side version / replaced order metadata）、`MarketMakerQuoteReconciliationReport`、`MarketMakerQuoteReconciliationIssue`、`HedgeOrderRequest`、`HedgeOrderResult`（含 retryable 錯誤分類）、`HedgeVenueIdempotencyRecord`、`HedgeVenueIdempotencyReport`、`HedgeVenueIdempotencyIssue`、`HedgeDecision`、`HedgeStrategyDecision`、`HedgeExecutionReport`、`HedgeDecisionAuditRecord`（含 internal trade ref）、`HedgeVenueFillMessage`、`HedgeFillRecord`（含 ledger ref）、`HedgeReconciliationReport`、`HedgeReconciliationIssue`。
- Recovery / validation：`Snapshot`、`RecoveryResult`、`ValidationIssue`。
- Polymarket：Gamma/CLOB/user WS 相關 request/response DTO；`PolymarketPlaceOrderRequest.clientRequestId` 是 CLOB place idempotency key，`PolymarketClobCommandRecord` 是 effectful CLOB command idempotency record。

注意：
- DTO 不應包含 repository 或 infrastructure 依賴。
- 新增 DTO 時要註明它是 command result、read model，還是外部 API schema。
