# domain/model/dto

跨層傳遞的資料模型與讀模型。

目前重點：
- Market data：`DepthDelta`、`MarketTicker`、`MarketKline`、`TradeTapeItem`。
- 風控/帳務：`AccountRiskSnapshot`、`MarkPriceSnapshot`、`FundingSettlementResult`、`LiquidationResult`。
- Snapshot read model：account risk snapshots、reconciliation reports、wallet ledger replay。
- Recovery / validation：`Snapshot`、`RecoveryResult`、`ValidationIssue`。
- Polymarket：Gamma/CLOB/user WS 相關 request/response DTO。

注意：
- DTO 不應包含 repository 或 infrastructure 依賴。
- 新增 DTO 時要註明它是 command result、read model，還是外部 API schema。
