# domain/repository/jpa

Spring Data JPA repositories。

目前狀態：
- Polymarket market/session/order/user WS/sync progress 主要走 JPA。
- 內部交易所核心熱狀態目前主要走 Redis repository。
- `JpaAdlExecutionStore` 保存 ADL forced execution summary / idempotency records。
- `JpaMarketDataSequenceCheckpointStore` 保存 market-data stream sequence/checksum checkpoints。
- `JpaMarketDataDepthDeltaStore` 保存 depth delta backfill records。
- `JpaMarketDataTradeTapeStore` 保存 restart-safe recent trade tape records。
- `JpaMarketDataTickerStore` 保存 restart-safe ticker latest-state records。
- `JpaMarketDataKlineStore` 保存 restart-safe kline records。
- `JpaHedgeVenueIdempotencyStore` 保存 hedge venue submit claim/result records。
- `JpaPolymarketClobCommandStore` 保存 Polymarket CLOB command claim/result records。

注意：
- JPA repository 是 infrastructure 依賴較重的例外；使用時要避免 domain service 直接被 persistence 細節綁死。
- production index 與 migration 需同步到 Flyway。
