# src/main/resources/static

靜態資源。

目前內容：
- `index.html`：本機真實交易測試控制台。
- `exchange.html`：prod-facing 前台交易操作台，覆蓋註冊/登入/登出、order book、下單、掛單、帳戶查詢；market selector 位於下單區並必須從 `/api/markets` 載入後台配置出的可交易市場，不讓使用者自由輸入未配置幣種，也不暴露任何後台導覽、做市商營運資訊、checksum 或 book version。前台主畫面只保留 Trade 分頁，維持 `Order Book` / `Place Order` 同一 row、`Open Orders` 下方；登入、註冊、資產摘要與登出全部集中在右上 profile drawer，未登入時顯示 email/password 登入卡片、主要登入 CTA 與次要註冊入口，並可依 `/api/auth/config` 顯示 Cloudflare Turnstile 相容免費真人驗證；註冊只建立 pending 帳號並提示完成 email verification，登入後才顯示個人資金、分類資訊、凍結資金、委託、持有倉位與歷史開關倉位，並用下拉複選和全選/全取消按鈕切換可見區塊；UID 只能由登入狀態帶出，不做可輸入欄位，帳戶資料由 session/auth refresh 更新，不提供手動帳戶 reload、市場資訊 reload 或 raw auth/account debug log；Open Orders refresh 使用 icon-only button 並保留多語系 accessibility label。Order book 只顯示一般市場深度、spread 與動態深度條，不標記做市商身份；前台使用單一 `/ws/exchange` multiplex WebSocket，未登入先送 `subscribe.market`，登入後同線路再送 `subscribe.user`，用公開 market / order lifecycle / trade signal 更新 depth 與 open orders，斷線或重連前才退回 1 秒 polling；`subscribe.user` 支援 opt-in `cancelOnDisconnect` 與 `resumeConnectionId`。
- `admin-console.html`：後台主入口，以 tabs 集中測試金、market config、risk parameters、market makers、DLQ；做市商後台以 embedded tab 顯示在同一個後台畫面，`exchange-admin.html` 保留為相容入口；後台外殼支援四語系並同步語言偏好到同源子頁。
- `admin-test-funds.html`：後台 MVP 測試金發放頁，搭配 `/api/admin/test-funds/airdrop` 與 `/api/margin/account`。
- `admin-market-config.html`：後台市場設定頁，額外顯示 depth best bid/ask、book version 與 checksum 等市場資料診斷。
- `admin-market-config.html`：admin market-config / fee settings 頁，搭配 `/api/admin/market-config` 與 `POST /api/admin/market-config/{symbol}/fees`。
- `admin-risk-parameters.html`：read-only admin risk parameter / switch 檢視頁，搭配 `/api/admin/risk-parameters`，首屏說明全域風控、oracle、margin tiers 與 liquidation controls 的用途。
- `exchange-admin.html`：admin 做市商 profile / strategy / hedge operation 內容頁，搭配 `/api/market-maker` profile、quote、hedge execution、reconciliation APIs；從 `admin-console.html` 以 `?embed=1` 開啟時會隱藏內層 header，避免像另一個後台畫面。
- `exchange.html`、`admin-console.html` 與 `exchange-admin.html`：支援 English、中文、Bahasa Malaysia、한국어 語言切換，偏好保存在 `localStorage.exchangeLanguage`；前台交易頁是 prod-facing 客戶入口，不暴露後台導覽。
- `admin-dlq.html`：read-only admin DLQ 檢視頁，搭配 `/api/admin/dlq`，payload / headers 只顯示 sanitizied preview，首屏說明 replay / quarantine / escalate 前的檢查用途。
- `admin-console.css`：admin 靜態頁共用視覺系統，統一 header、navigation、panel、table、form、status 與 notice 樣式。

注意：
- 這不是 production 前端。
- 若新增正式前端，應先確認 build pipeline、auth flow、secret handling。
- 客戶註冊安全設定集中在 `customer-auth.*`：本機預設關閉 email verification / captcha，prod profile 預設開啟 email verification 與 Turnstile 相容 captcha；captcha secret 不可寫入靜態頁，email verification token 只存 SHA-256 hash。
- 靜態頁新增或改版時，應補 `tests/e2e/` 的 Playwright browser smoke，至少覆蓋頁面載入、主要控制項、API 成功資料渲染與錯誤可視狀態。
