# test interfaces/web/interceptor

Interceptor tests。

目前內容：
- `RequestLoggingInterceptorTest`：request/correlation id 和 MDC lifecycle。
- `ApiAuthenticationInterceptorTest`：API key/JWT auth 與 permission enforcement。
- `ProtectedApiSecurityInterceptorTest`：IP allowlist、rate limit、安全審計。

注意：
- Interceptor 變更容易影響所有 API，測試需涵蓋 allow/reject 分支。
