# src/main/resources/static

靜態資源。

目前內容：
- `index.html`：本機真實交易測試控制台。
- `exchange.html`：前台內部交易所操作台，覆蓋註冊/登入/登出、order book、下單、掛單、帳戶查詢；market selector 必須從 `/api/markets` 載入後台配置出的可交易市場，不讓使用者自由輸入未配置幣種。前台版型應把使用者摘要放在上方，`Place Order` / `Order Book` / `Open Orders` 放在同一個訂單工作區；UID 只能由登入狀態帶出，不做可輸入欄位。
- `admin-console.html`：後台主入口，以 tabs 集中測試金、market config、risk parameters、DLQ；使用者導覽應只暴露這一個後台頁。
- `admin-test-funds.html`：後台 MVP 測試金發放頁，搭配 `/api/admin/test-funds/airdrop` 與 `/api/margin/account`。
- `admin-market-config.html`：admin market-config / fee settings 頁，搭配 `/api/admin/market-config` 與 `POST /api/admin/market-config/{symbol}/fees`。
- `admin-risk-parameters.html`：read-only admin risk parameter / switch 檢視頁，搭配 `/api/admin/risk-parameters`，首屏說明全域風控、oracle、margin tiers 與 liquidation controls 的用途。
- `admin-dlq.html`：read-only admin DLQ 檢視頁，搭配 `/api/admin/dlq`，payload / headers 只顯示 sanitizied preview，首屏說明 replay / quarantine / escalate 前的檢查用途。
- `admin-console.css`：admin 靜態頁共用視覺系統，統一 header、navigation、panel、table、form、status 與 notice 樣式。

注意：
- 這不是 production 前端。
- 若新增正式前端，應先確認 build pipeline、auth flow、secret handling。
- 靜態頁新增或改版時，應補 `tests/e2e/` 的 Playwright browser smoke，至少覆蓋頁面載入、主要控制項、API 成功資料渲染與錯誤可視狀態。
