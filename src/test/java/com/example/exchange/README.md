# test com.example.exchange

測試 package root。

目前測試範圍：
- `ExchangeApplicationTests`：Spring context smoke test。
- `application/service/`：帳務、風控、outbox、metrics、risk snapshot。
- `infra/matching/`：in-memory matching engine。
- `domain/util/`：checksum、log sanitizer。
- `interfaces/web/`：security、interceptor。

目前狀態：
- 測試偏 focused unit/integration style，避免依賴完整外部服務。
- 大多數 repository 在測試內使用 in-memory stub。
