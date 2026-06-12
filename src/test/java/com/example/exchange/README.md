# test com.example.exchange

測試 package root。

目前測試範圍：
- `ExchangeApplicationTests`：P0 application class smoke test。
- `application/service/`：P0 帳務、風控、認證、做市商報價、command transaction boundary。
- `infra/matching/`：P0 in-memory matching engine。
- `domain/util/`：P0 log sanitizer。
- `interfaces/web/`：P0 API auth、protected API security、WebSocket 與 public market API。

只保留 P0 可執行測試；P1-P5 的已刪測試與恢復依據記錄在 `docs/ai/test-priority.md`。

測試類別索引：

| 測試類別 | 主要驗證 |
| --- | --- |
| `ExchangeApplicationTests` | application class 能載入，避免啟動入口破壞 Spring 掃描。 |
| `AccountRiskServiceTest` | risk snapshot 的 equity、PNL、maintenance margin、risk ratio。 |
| `AuthServiceTest` | local registration/login/logout、email verification token 與 session revocation。 |
| `CommandTransactionBoundaryTest` | command body 失敗時 rollback command transaction。 |
| `HumanVerificationServiceTest` | 註冊真人驗證 disabled/dev bypass/fail-closed 行為。 |
| `MarginServiceTest` | deposit/withdraw transfer state machine、callback idempotency、manual-review owner 與 ledger side effect。 |
| `MarketMakerAutoQuoteServiceTest` | 自動做市 quote runner baseline。 |
| `MarketMakerQuoteLifecycleServiceTest` | 做市 quote state lifecycle。 |
| `MarketMakerQuoteServiceTest` | 做市 quote command validation 與 kill switch。 |
| `OrderAccountingIntegrationTest` | 下單到撮合、帳務、position、market data、lifecycle event、pre-trade frequency limit 的整合流程。 |
| `SensitiveLogSanitizerTest` | query、JSON、Authorization header、known sensitive header 遮罩。 |
| `InMemoryMatchingEngineTest` | FIFO、post-only、自成交拒絕、FOK/IOC、市價單流動性不足、snapshot/replay、multi-symbol replay validation。 |
| `WebSocketPushConfigTest` | `/ws/exchange` user subscription、resume 與 cancel-on-disconnect。 |
| `MarketControllerTest` | public market API sorted symbols and no admin-only fields。 |
| `ApiAuthenticationInterceptorTest` | API auth 開關、401、403、role/scope 授權、principal 寫入。 |
| `ProtectedApiSecurityInterceptorTest` | IP allowlist 與 per-IP rate limit。 |
| `ApiKeyAuthenticatorTest` | API key hash 驗證與 roles/scopes 解析。 |
| `JwtAuthenticatorTest` | HS256 JWT 簽章、exp、roles/scopes。 |
| `ProtectedApiClassifierTest` | protected endpoint classification。 |

目前狀態：
- 測試偏 focused unit/integration style，避免依賴完整外部服務。
- 大多數 repository 在測試內使用 in-memory stub。
