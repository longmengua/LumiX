# P29-R12：受限綁定裝置平台槽位

## 任務目的

將可免裝置驗證的受信任裝置改為每個帳戶各一台桌上型電腦、平板與手機的三個平台槽位。這是 authentication/security runtime 變更，不是 MFA、完整裝置指紋、風控、production launch 或真實資金功能。

## 完成內容

- `V014` 在 `user_login_devices` 與待核准登入請求保存 `DESKTOP`、`TABLET`、`MOBILE` 平台槽位，並以 partial unique index 保證每個帳戶每個平台最多一台 active device。
- `V015` 保存同平台裝置替換後的資金外流限制截止時間，預設為 24 小時；它涵蓋未來的外部提幣與帳戶間轉帳 gate，但不建立任何提款或轉帳 runtime。
- 登入 request 的 User-Agent 只用來做粗粒度平台分類；完整 User-Agent 仍不保存，且平台分類不取代 device cookie 高熵秘密、fingerprint digest 與 email 一次性核准。
- 同類別尚未綁定任何裝置時，登入會在鎖住使用者列後直接填入空槽位；兩個併發登入不能同時取得同一空槽位。
- 同類別已有裝置時，即使舊的通知偏好曾關閉，新的 device cookie 一律需要 email Yes／No。通知偏好 API 與畫面已移除，避免提供與強制換機核准相衝突的假開關。
- email Yes 只核准原始登入瀏覽器的候選裝置；確認信開啟的瀏覽器不會被綁定或取得 session。原始瀏覽器以 HttpOnly pending cookie 輪詢完成登入，並原子撤銷同平台舊 device 與其 active session。
- 同平台候選裝置完成替換時，server 只可延長 `fund_transfer_restricted_until` 至至少 24 小時後；個人中心倒數僅供顯示，未來提款與帳戶間轉帳入口必須各自用 server 時間 fail closed。
- 個人中心總覽與安全頁都顯示去敏的已綁定裝置平台、名稱、IP 與最後使用時間；總覽最多三列並提供安全頁管理入口。

## 安全不變式

```text
每帳戶：DESKTOP 1 台 + TABLET 1 台 + MOBILE 1 台

新登入裝置的平台槽位為空
    |
    +-- 鎖住使用者列 --> 建立 device + session

新登入裝置的平台槽位已占用
    |
    +-- email Yes/No --> Yes 只核准原始候選裝置
                              |
                              +-- 原始瀏覽器 pending cookie
                                      |
                                      +-- 撤銷舊 device + sessions
                                      +-- 延長 24 小時資金外流限制
                                      +-- 建立候選 device + session
```

## Migration 與 rollback

`V014` 先以既有去敏裝置標籤保守分類歷史資料，再保留每個平台最後使用的一台；較舊同平台 device 與其 active session 會一併撤銷，才能建立資料庫唯一限制。`V015` 只增加可延長的安全截止時間，不修改餘額、帳本或提款狀態。此影響是套用一槽一台規則的必要結果，部署前需通知帳戶可能要重新登入。

Flyway migration 是 append-only，不能以 application rollback 移除 `device_platform`、check constraint 或唯一 index。rollback 時只能停止新的 UI/API deployment；不得恢復舊的多同平台 active device 行為或重新啟用會繞過 email 換機核准的通知偏好。

## 驗證

```text
PASS  UserAuthenticationServiceLoginSecurityTest：空平台槽位直接綁定
PASS  UserAuthenticationServiceLoginSecurityTest：同平台新裝置必須寄送 email 核准
PASS  UserAuthenticationServiceLoginSecurityTest：email Yes 不綁定確認信瀏覽器
PASS  UserAuthenticationServiceLoginSecurityTest：原始候選裝置取代同平台舊 device/session
PASS  P12T09SchemaVerificationTest：V001–V014 在乾淨 PostgreSQL schema 完整套用
PASS  web npm run typecheck
PASS  web npm run build
```

## 明確未完成

- UA spoofing 防護、passkey、MFA、裝置命名、完整裝置管理歷程、IP reputation、異常登入偵測、rate limit、SIEM、TLS ingress、HSTS、CSRF 與 production security review。
- 任何交易、資產、入金、提款、帳本或 production launch。

## 風險與人工審核

`HUMAN_REVIEW_REQUIRED: yes`。人工審核應確認：平台分類不可當作不可偽造 fingerprint、空槽位檢查受 user row lock 與資料庫唯一索引保護、email Yes 不能在確認頁建立 session、原始 pending cookie 才可完成登入、同平台舊 device/session 必定一併撤銷，以及 `V014` 對既有重複裝置造成的登出影響已被接受。
