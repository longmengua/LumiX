# Phase 29 - 公開與私有 API 強化

## 狀態

```text
COMPLETED_FOR_API_ADMISSION_CONTRACT_FOUNDATION
```

後續 runtime track：`P29-R01 API Runtime Infrastructure Topology Foundation` 已依人類指示完成 infrastructure foundation，狀態為 `COMPLETED_FOR_INFRASTRUCTURE_FOUNDATION`；詳見 `p29-r01-infrastructure-topology-foundation.md`。它只建立部署與 topology foundation，不啟用任何公開 auth、資金或交易 API。

`P29-R02 Persistent User Authentication Runtime` 已完成 persistent authentication runtime foundation：PostgreSQL credential/session、HttpOnly Cookie 登入登出與密碼變更，以及受控 SMTP 密碼重設邊界；詳見 `p29-r02-persistent-user-auth-runtime.md`。此項目屬 authentication/security change，`HUMAN_REVIEW_REQUIRED: yes`；未提供 SMTP/TLS/secret manager 時不得當作對外帳號系統啟用。

`P29-R03 API Authentication and Transport Security Gate` 已完成 security gate foundation：`/api/v1/**` 預設 deny 鑒權、HTTP API fail-closed deployment switch 與 no-store/security headers 均已驗證。TLS ingress、憑證、HSTS、CSRF/rate-limit 與 public launch 尚未完成；`HUMAN_REVIEW_REQUIRED: yes`。

`P29-R04 Authenticated Login History API` 已完成目前使用者自己的成功登入紀錄 read API 與前端帳戶頁接線；詳見 `p29-r04-authenticated-login-history-api.md`。此項目不蒐集 IP、user agent 或 session secret，且不是完整 audit/security event runtime；`HUMAN_REVIEW_REQUIRED: yes`。

## Phase charter

建立已批准 domain contract 的版本化 API admission foundation；不把 OpenAPI metadata、mock 或 sandbox route 當成可公開的 production service。

## 高層任務

1. API inventory、versioning、compatibility/deprecation 與明確 read/write contract：`COMPLETED_FOR_CONTRACT`。
2. Authentication、API key scope、session/nonce、authorization 與敏感 endpoint 職責分離：`BOUNDARY_ONLY`，沒有 auth runtime。
3. Idempotency、concurrency、error taxonomy、precision serialization、time/health semantics：`COMPLETED_FOR_ADMISSION_CONTRACT`。
4. Rate limit、abuse/DDoS protection、pagination/filtering bounds、request validation 與安全 logging：`COMPLETED_FOR_RATE_GATE_CONTRACT`，沒有 transport enforcement。
5. Contract/integration/security test、consumer migration、rollback/version coexistence evidence：`COMPLETED_FOR_FOUNDATION`。

## Gate

`HUMAN_REVIEW_REQUIRED: yes`；未完成 P26–P28 review、安全設計與每個 command 的個別核准前，不得對外啟用資金或交易 endpoint。
