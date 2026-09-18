# P29-R05 — Loopback 密碼重設信件寄送

## 狀態

```text
COMPLETED_FOR_LOOPBACK_DEVELOPMENT_RUNTIME
HUMAN_REVIEW_REQUIRED: yes
```

## 目標

讓同一台開發機器可用 Gmail SMTP 收到並開啟密碼重設信，完成忘記密碼的本機端對端驗證；同時不把一般 HTTP 或公開 HTTP endpoint 變成 reset token 的傳輸 bypass。

## 已納入範圍

- 新增預設為 `false` 的 `LUMIX_AUTH_PASSWORD_RESET_ALLOW_LOOPBACK_HTTP`。
- SMTP 啟用時，HTTPS URL 一律可用；HTTP 只有在開關為 `true` 且 host 精確為 `localhost`、`127.0.0.1` 或 `::1` 時可用。
- 拒絕含 user-info、query 或 fragment 的 base URL，避免組裝 reset URL 時產生模糊目的地。
- 本機 `.env` 的 Gmail App Password 只供 Docker Compose 注入，已由 Git 忽略；不得寫入範例、文件或提交。
- 最高管理員首次啟用信與既有管理員復原信都導向後台專用重設頁；API 僅接受既有 `PENDING_ACTIVATION` 或 `ACTIVE` 的最高管理員 principal，成功後於同一 transaction 更新密碼、撤銷 session、消耗 token，並只在需要時啟用 pending principal。一般使用者 token 仍會拒絕。

## 安全邊界

```text
Gmail SMTP + STARTTLS

最高管理員 bootstrap 可透過 `LUMIX_ADMIN_SUPER_ADMIN_LOCALE` 指定首次啟用信語系；留白時為 `en-US`，目前只接受已有受控文案的 `en-US` 與 `zh-TW`。未知值會 fail-closed，避免高權限啟用信以猜測語言寄送；此設定不保存或偽造管理員的前端語言偏好。
        |
        v
email contains reset token link
        |
        v
only same-machine browser may use http://127.0.0.1 / localhost / ::1
        |
        v
POST /api/v1/auth/password/reset
```

SMTP 加密只保護 server 到 Gmail；reset token 被使用者瀏覽器帶回 API 的鏈路仍需 HTTPS。loopback 流量不離開同機，才可作為受控開發例外；任何 LAN、IP、網域或公開 HTTP URL 都會 fail closed。

## 明確不含範圍

- 公開 HTTP deployment、production SMTP credential、secret manager、DKIM/SPF/DMARC、email delivery webhook、rate limit、MFA、email verification 或 security-event audit。
- production-ready 或 production launch 宣稱。

## 驗證

```text
2026-09-09
PASS  Docker server image build（含 Java 編譯）
PASS  Compose 以本機 ignored .env 啟動 server，SMTP 與 loopback URL 啟動驗證通過
PASS  /actuator/health -> UP（SMTP health 已啟用）
PASS  註冊 Gmail 測試別名後呼叫 POST /api/v1/auth/password/forgot -> 202
PASS  delivery adapter 以同步 SMTP send 執行；寄送失敗會 rollback transaction，202 代表 Gmail SMTP 已接受信件
PENDING HUMAN  在同一台 Mac 的 Gmail 信箱開啟實際信件並點擊 reset link，完成一次 UI reset；token 不會寫入終端、log 或測試紀錄
2026-09-18
PASS  發現並修正首次啟用信原本會被僅限 `ACTIVE` principal 的後台 reset gate 拒絕；待啟用管理員已由窄範圍 service test 覆蓋，且一般使用者 token 仍被拒絕
```

## 人工審核重點

`HUMAN_REVIEW_REQUIRED: yes`。請確認開關預設關閉、loopback host allowlist 沒有 wildcard、一般 HTTP URL 不可用、App Password 未進 Git，且 public deployment 仍要求 HTTPS。
