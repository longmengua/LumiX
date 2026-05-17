# interfaces/web/interceptor

Spring MVC interceptors。

目前內容：
- `RequestLoggingInterceptor`：request/correlation id、MDC、request lifecycle log。
- `ApiAuthenticationInterceptor`：API key / JWT authentication and authorization。
- `ProtectedApiSecurityInterceptor`：protected API IP allowlist、rate limit、安全審計。

注意：
- Interceptor 應只做橫切關注點，不寫業務狀態。
- 新 protected endpoint 要確認 `ProtectedApiClassifier` 分類。
