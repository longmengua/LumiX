# test com.example.exchange

測試 package root。

目前測試範圍：
- `ExchangeApplicationTests`：Spring context smoke test。
- `application/service/`：帳務、風控、outbox、metrics、risk snapshot。
- `infra/matching/`：in-memory matching engine。
- `domain/util/`：checksum、log sanitizer。
- `interfaces/web/`：security、interceptor。

測試類別索引：

| 測試類別 | 主要驗證 |
| --- | --- |
| `ExchangeApplicationTests` | application class 能載入，避免啟動入口破壞 Spring 掃描。 |
| `AccountRiskServiceTest` | risk snapshot 的 equity、PNL、maintenance margin、risk ratio。 |
| `MarginServiceTest` | deposit/withdraw transfer state machine 與 ledger side effect。 |
| `OperationalMetricsServiceTest` | 下單結果、撤單、成交事件、延遲統計。 |
| `OrderAccountingIntegrationTest` | 下單到撮合、帳務、position、market data、lifecycle event 的整合流程。 |
| `OutboxServiceTest` | outbox retry、DLQ、replay、manual compensation、trace headers。 |
| `RiskSettlementServiceTest` | funding、liquidation、insurance fund、account reconciliation。 |
| `OrderBookChecksumTest` | order book checksum 的 BigDecimal scale normalization。 |
| `SensitiveLogSanitizerTest` | query、JSON、Authorization header、known sensitive header 遮罩。 |
| `InMemoryMatchingEngineTest` | FIFO、post-only、自成交拒絕、FOK/IOC、市價單流動性不足。 |
| `ApiAuthenticationInterceptorTest` | API auth 開關、401、403、role/scope 授權、principal 寫入。 |
| `ProtectedApiSecurityInterceptorTest` | IP allowlist 與 per-IP rate limit。 |
| `RequestLoggingInterceptorTest` | request/correlation id、response headers、MDC lifecycle。 |
| `ApiKeyAuthenticatorTest` | API key hash 驗證與 roles/scopes 解析。 |
| `IpAllowlistTest` | 精確 IP、萬用字元、IPv4 CIDR、拒絕分支。 |
| `JwtAuthenticatorTest` | HS256 JWT 簽章、exp、roles/scopes。 |

目前狀態：
- 測試偏 focused unit/integration style，避免依賴完整外部服務。
- 大多數 repository 在測試內使用 in-memory stub。
