# test interfaces/web

Web layer tests。

目前目錄：
- `interceptor/`：request logging、API auth、protected API security。
- `security/`：API key、JWT、IP allowlist。

注意：
- 新 protected endpoint 或 auth 規則變更，要同步補 interceptor/security tests。
- `/api/market-maker/**` 屬於 admin-only API，已由 `ApiAuthenticationInterceptorTest` 覆蓋 trader key 拒絕情境。
