# test interfaces/web/interceptor

Interceptor tests。

目前內容：

| 測試類別 | 主要案例 |
| --- | --- |
| `RequestLoggingInterceptorTest` | incoming trace headers、response headers、MDC cleanup、缺 headers 時產生 ids。 |
| `ApiAuthenticationInterceptorTest` | auth disabled、missing credentials、admin/trader 授權、`/api/admin/**` admin-only、principal attribute。 |
| `ProtectedApiSecurityInterceptorTest` | IP allowlist 拒絕/放行、per-IP rate limit 與 Retry-After。 |

注意：
- Interceptor 變更容易影響所有 API，測試需涵蓋 allow/reject 分支。
