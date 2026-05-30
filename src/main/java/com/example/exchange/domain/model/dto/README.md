# domain/model/dto

跨層傳遞的資料模型與讀模型。

目前重點：
- Market data：`DepthDelta`、`MarketTicker`、`MarketKline`、`TradeTapeItem`。
- 風控/帳務：`AccountRiskSnapshot`、`MarkPriceSnapshot`、`FundingSettlementResult`、`LiquidationResult`、`BonusCreditGrant`、`BonusCreditReport`、`BonusCreditCampaignReport`、`TurnoverRecord`、`TurnoverSummary`、`TurnoverReconciliationReport`。
- Snapshot read model：account risk snapshots、reconciliation reports、wallet ledger replay。
- Finance read model：`TrialBalanceReport`、`TrialBalanceLine`、`LedgerReplayComparisonReport`、`LedgerReplayComparisonIssue`。
- Market maker / hedging：`MarketMakerProfile`、`MarketMakerRiskLimit`、`MarketMakerExposure`、`MarketMakerQuoteCommand`、`MarketMakerQuoteDecision`、`HedgeOrderRequest`、`HedgeOrderResult`（含 retryable 錯誤分類）、`HedgeVenueIdempotencyRecord`、`HedgeDecision`、`HedgeStrategyDecision`、`HedgeExecutionReport`、`HedgeDecisionAuditRecord`、`HedgeVenueFillMessage`、`HedgeFillRecord`、`HedgeReconciliationReport`、`HedgeReconciliationIssue`。
- Recovery / validation：`Snapshot`、`RecoveryResult`、`ValidationIssue`。
- Polymarket：Gamma/CLOB/user WS 相關 request/response DTO；`PolymarketPlaceOrderRequest.clientRequestId` 是 CLOB place idempotency key，`PolymarketClobCommandRecord` 是 effectful CLOB command idempotency record。

注意：
- DTO 不應包含 repository 或 infrastructure 依賴。
- 新增 DTO 時要註明它是 command result、read model，還是外部 API schema。
