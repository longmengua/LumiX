# P29-R03 — API 鑒權與傳輸安全 Gate

## 狀態

```text
COMPLETED_FOR_SECURITY_GATE_FOUNDATION
HUMAN_REVIEW_REQUIRED: yes
```

## 目標

對 `/api/v1/**` 建立預設拒絕的 session 鑒權，並以 HTTPS transport gate 保護帳密、Cookie 與 API payload 的傳輸機密性與完整性。這不是前端自製 body 加解密，也不會宣稱完成 production TLS deployment。

## 範圍

- 匿名白名單僅限 register、login、forgot password、reset password，以及同樣受 CAPTCHA 與固定 admin principal gate 保護的後台 forgot/reset password。
- 其餘 `/api/v1/**` endpoint 於進入 controller 前驗證 HttpOnly session，並建立去敏 principal attribute。
- `LUMIX_SECURITY_REQUIRE_HTTPS=true` 時拒絕非 HTTPS API request；同時要求 secure Cookie。
- API 的 no-store、nosniff、frame deny、no-referrer response headers。
- 維持 local Compose HTTP 開發模式，但明確不能用於對外 deployment。

## 不含範圍

- 私有 payload 加密、客戶端持有對稱金鑰、以 JWT 取代 server-side 可撤銷 session。
- TLS 私鑰、憑證申請、ACME、load balancer、HSTS 實際發布。
- CSRF token、rate-limit、WAF/DDoS、MFA、KYC/AML、資金與交易 endpoint。

## 已完成驗證

```text
2026-09-09
PASS  docker compose config --quiet 與 server image build
PASS  /actuator/health -> 200
PASS  匿名 /api/v1/private-check -> 401（預設 deny）
PASS  匿名 register -> 201；有效 session /api/v1/auth/me -> 200
PASS  API response: Cache-Control no-store、nosniff、frame deny、no-referrer headers
PASS  LUMIX_SECURITY_REQUIRE_HTTPS=true 且 Cookie Secure=false 時 server 拒絕啟動
2026-09-14
PASS  後台登入維持 `/admin/login` 專用畫面，沒有前台登入跳轉或後台自助註冊入口
PASS  `/api/admin/v1/auth/password/forgot` 對非管理員維持相同 accepted 回應，且不建立 reset token
PASS  `/api/admin/v1/auth/password/reset` 拒絕一般使用者 token；ACTIVE admin 才可使用專用 `/admin/reset-password` 一次性連結
PASS  `UserAuthenticationServiceSuperAdminPasswordRecoveryTest`、`SmtpPasswordResetDeliveryTest` 通過
```

## 阻擋條件

- 需要人類／基礎設施提供：正式 DNS、CA 憑證、TLS ingress、secret manager、網路 ACL 與 TLS 1.2/1.3 policy evidence。
- TLS、鑒權、Cookie 或管理員密碼復原改動屬 security change，合併前必須保留 `HUMAN_REVIEW_REQUIRED`。
