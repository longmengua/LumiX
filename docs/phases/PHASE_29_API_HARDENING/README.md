# Phase 29 - 公開與私有 API 強化

## 狀態

```text
COMPLETED_FOR_API_ADMISSION_CONTRACT_FOUNDATION
```

後續 runtime track：`P29-R01 API Runtime Infrastructure Topology Foundation` 已依人類指示完成 infrastructure foundation，狀態為 `COMPLETED_FOR_INFRASTRUCTURE_FOUNDATION`；詳見 `p29-r01-infrastructure-topology-foundation.md`。它只建立部署與 topology foundation，不啟用任何公開 auth、資金或交易 API。

`P29-R02 Persistent User Authentication Runtime` 已完成 persistent authentication runtime foundation：PostgreSQL credential/session、HttpOnly Cookie 登入登出與密碼變更，以及受控 SMTP 密碼重設邊界；詳見 `p29-r02-persistent-user-auth-runtime.md`。此項目屬 authentication/security change，`HUMAN_REVIEW_REQUIRED: yes`；未提供 SMTP/TLS/secret manager 時不得當作對外帳號系統啟用。

`P29-R03 API Authentication and Transport Security Gate` 已完成 security gate foundation：`/api/v1/**` 預設 deny 鑒權、HTTP API fail-closed deployment switch 與 no-store/security headers 均已驗證。TLS ingress、憑證、HSTS、CSRF/rate-limit 與 public launch 尚未完成；`HUMAN_REVIEW_REQUIRED: yes`。

`P29-R04 Authenticated Login History API` 已完成目前使用者自己的成功登入紀錄 read API 與前端帳戶頁接線；詳見 `p29-r04-authenticated-login-history-api.md`。P29-R08 僅向本人追加 IP 與去敏裝置快照；不回傳完整 user agent 或 session secret，且不是完整 audit/security event runtime；`HUMAN_REVIEW_REQUIRED: yes`。

`P29-R05 Loopback Password Reset Delivery` 已完成同機開發用的 loopback HTTP reset link 例外與 Gmail SMTP 實際寄送 acceptance 驗證；詳見 `p29-r05-loopback-password-reset-delivery.md`。預設仍拒絕 HTTP，只有顯式開關且精確為 localhost／127.0.0.1／::1 時可用；UI 點擊 token 的最後確認仍待人類在 Gmail 收件匣執行；`HUMAN_REVIEW_REQUIRED: yes`。

`P29-R06 Authenticated Personal Profile API` 已完成目前登入者本人 profile 的讀取與顯示名稱更新，並將個人中心總覽接至真實資料；詳見 `p29-r06-authenticated-personal-profile-api.md`。KYC、資產、劃轉、API Key 與偏好設定仍是既有 mock，不得誤認為已整合；`HUMAN_REVIEW_REQUIRED: yes`。

`P29-R07 Login History Cursor Pagination` 已完成登入紀錄每頁預設 10 筆的 before／after／anchor cursor API 與前端雙向無限捲動；詳見 `p29-r07-login-history-cursor-pagination.md`。URL anchor 讓重新整理保留目前查看的登入時間窗口；它不是完整 security event runtime；`HUMAN_REVIEW_REQUIRED: yes`。

`P29-R08 New Device Login Verification` 已完成受信任裝置、未知／變更 browser fingerprint 的 email Yes／No 確認，以及登入歷程的本人 IP／去敏裝置快照；詳見 `p29-r08-new-device-login-verification.md`。email 決定不會直接建立 session，只有原登入瀏覽器的 HttpOnly pending cookie 與 fingerprint 相符才可完成一次登入；`HUMAN_REVIEW_REQUIRED: yes`。

`P29-R09 Slider CAPTCHA and Registration Bloom Filter` 已完成 Redis-backed server-side 滑動驗證、用途／browser fingerprint 綁定的一次性通行 token，並於註冊流程加入 Bloom filter 快速預檢；詳見 `p29-r09-slider-captcha-registration-bloom-filter.md`。資料庫 unique constraint 仍是重複 email 的最終裁決；Redis 不可用時驗證碼 fail-closed；`HUMAN_REVIEW_REQUIRED: yes`。

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
