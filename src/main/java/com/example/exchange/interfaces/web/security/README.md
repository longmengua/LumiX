# interfaces/web/security

Web security helper。

目前內容：
- `ApiKeyAuthenticator`、`JwtAuthenticator`。
- `IpAllowlist`。
- `ProtectedApiClassifier` / `ProtectedApiCategory`。
- `ApiPrincipal`。
- `UserStreamSubscriptionAuthorizer`：private user SSE/WebSocket stream 訂閱授權。
- `MarketDataStreamRateLimiter`：market-data SSE/WebSocket per-client stream 訂閱限流。
- `MarketMakerQuoteRateLimiter`：market-maker quote command per-client / market-maker / symbol 限流。
- `MarketMakerHedgeExecutionRateLimiter`：market-maker manual hedge execution per-client / execution scope 限流。
- `MarketMakerEndpointAuditLogger`：market-maker effectful endpoint audit 欄位，包含 operator identity 與 approval token outcome 分類。
- `/api/admin/**` 由 `ProtectedApiClassifier` 分類為 `ADMIN`。

目前狀態：
- API key、JWT、IP allowlist、固定視窗 rate limit、private user stream authorization baseline、market-data stream per-client limiter、market-maker quote command limiter、market-maker hedge execution limiter、market-maker endpoint audit fields 已有測試。
- production 仍需完整 key rotation、audit sink、異常使用偵測。

注意：
- 不要在 log 或 exception message 暴露 credential raw value。
