# domain/repository

Repository contracts，domain/application 只依賴這裡的介面。

目前重點：
- 內部交易所：`OrderRepository`、`AccountRepository`、`PositionRepository`、`WalletLedgerRepository`、`WalletLedgerJournal`、`AccountRiskSnapshotStore`。
- Market maker / hedging：`MarketMakerQuoteStateStore` 保存 active quote ownership；`HedgeVenueIdempotencyStore` 保存外部 venue submit 的 refId claim/result。
- Reliability：`OutboxRepository`、`DlqRepository`、`EventStore`、`SnapshotRepository`。
- Polymarket：`PolymarketClobCommandStore` 保存 CLOB effectful command 的 commandId claim/result；JPA repository 位於 `jpa/`，Gamma client contract 位於 `client/`。

注意：
- 新增 repository 先定義 contract，再到 `infra/` 或 `domain/repository/jpa` 放實作。
- contract 不應暴露 Redis key、Kafka topic 或 JPA query 細節。
