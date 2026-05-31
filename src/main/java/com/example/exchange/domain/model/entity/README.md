# domain/model/entity

核心狀態模型。

目前重點：
- 內部交易所：`Order`、`Account`、`Position`、`Symbol`、`SymbolConfig`（含 risk tiers）。
- 帳務：`WalletLedgerEntry`、`WalletLedgerPosting`、`WalletTransfer`、`BonusCreditGrantRecord`、`TurnoverRecordEntity`。
- 訂單 lifecycle：`OrderLifecycleEventRecord`、`OrderLifecycleProjection` 保存 latest order state 與 strategy/market-maker tags。
- 做市商：`MarketMakerProfileRecord`、`MarketMakerRiskLimitRecord`、`MarketMakerQuoteStateRecord`、`HedgeDecisionAuditRecordEntity`（含 internal trade ref）、`HedgeFillRecordEntity`（含 ledger ref）。
- 風控 read model：`AccountRiskSnapshotRecord`、`AdlExecutionRecordEntity`。
- Market data：`MarketDataSequenceCheckpointRecord`、`MarketDataDepthDeltaRecord`、`MarketDataTradeTapeRecord`、`MarketDataTickerRecord`、`MarketDataKlineRecord`。
- Reliability：`OutboxEvent`、`DlqEvent`。
- Polymarket：market、session、local order、WS event、sync progress entities。

目前狀態：
- 多數 entity 可被 Redis/JPA/Jackson 序列化。
- production 前要補正式 schema、索引、版本與 migration 策略。

注意：
- 這裡的 method 可承載 domain state transition，但不要直接呼叫 repository。
