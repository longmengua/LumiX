# interfaces/web/security

Web security helper。

目前內容：
- `ApiKeyAuthenticator`、`JwtAuthenticator`。
- `IpAllowlist`。
- `ProtectedApiClassifier` / `ProtectedApiCategory`。
- `ApiPrincipal`。

目前狀態：
- API key、JWT、IP allowlist、固定視窗 rate limit baseline 已有測試。
- production 仍需完整 key rotation、audit sink、異常使用偵測。

注意：
- 不要在 log 或 exception message 暴露 credential raw value。
