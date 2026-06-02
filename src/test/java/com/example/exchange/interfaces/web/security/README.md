# test interfaces/web/security

Security helper tests。

目前內容：

| 測試類別 | 主要案例 |
| --- | --- |
| `ApiKeyAuthenticatorTest` | sha256 API key 驗證、subject、roles、scopes、unknown key 拒絕。 |
| `JwtAuthenticatorTest` | HS256 JWT 簽章驗證、exp 過期拒絕、roles/scopes 解析。 |
| `IpAllowlistTest` | 精確 IP、`*`、IPv4 CIDR、網段外/無效規則/空值拒絕。 |
| `MarketMakerQuoteRateLimiterTest` | 做市商 quote API frequency limit、client/market-maker/symbol key、forwarded IP、停用開關。 |
| `MarketMakerHedgeExecutionRateLimiterTest` | 做市商 hedge execution API frequency limit、client/execution scope key、forwarded IP、停用開關。 |
| `MarketMakerEndpointAuditLoggerTest` | 做市商 effectful endpoint audit 欄位、operator identity、request id、approval token outcome 分類。 |
| `ProtectedApiClassifierTest` | `/api/admin/**` 分類為 admin-only API。 |

注意：
- 認證邏輯改動要保持 secret masking、permission scope、過期/格式錯誤的測試覆蓋。
