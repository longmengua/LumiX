# domain/repository/jpa

Spring Data JPA repositories。

目前狀態：
- Polymarket market/session/order/user WS/sync progress 主要走 JPA。
- 內部交易所核心熱狀態目前主要走 Redis repository。

注意：
- JPA repository 是 infrastructure 依賴較重的例外；使用時要避免 domain service 直接被 persistence 細節綁死。
- production index 與 migration 需同步到 Flyway。
