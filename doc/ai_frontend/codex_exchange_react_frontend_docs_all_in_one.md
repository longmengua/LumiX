# React 交易所前端畫面 Codex Mini 分批開工文件合集



---

# FILE: 00_REACT_FE_README_投餵順序.md

# React 前端畫面 Codex Mini 投餵順序總索引

這組文件專門用來讓 Codex mini 以 **React + TypeScript** 一份一份建立交易所前端畫面。

預設技術方向：

| 項目 | 建議 |
|---|---|
| 前端框架 | React |
| 語言 | TypeScript |
| 建置工具 | Vite 優先；若 repo 已有 React 架構則沿用 |
| 路由 | React Router；若 repo 已有路由則沿用 |
| 狀態管理 | 先用 React hooks / context；不要一開始引入大型狀態庫 |
| UI | 優先沿用現有 UI；沒有就先用乾淨 CSS / Tailwind，如果 repo 已有 |
| API | 先 mock service / adapter |
| 圖表 | K 線先放容器，不急著接圖表庫 |

---

## 投餵原則

```text
一次只做一個頁面群。
資料先用 mock service。
不要接真實撮合。
不要接真實強平。
不要接真實錢包掃鏈。
不要新增大型套件，除非 repo 已經使用。
不要重構無關模組。
所有頁面要有 loading、empty、error 狀態。
交易、資產、API key 等敏感資訊要脫敏或格式化。
```

---

## 建議投餵順序

| 順序 | 文件 | 用途 |
|---:|---|---|
| 1 | 01_react_repo_scan.md | 掃描 React repo，不改檔 |
| 2 | 02_react_app_setup_rules.md | React 專案規則與目錄約定 |
| 3 | 03_react_design_system_components.md | 共用元件與格式化工具 |
| 4 | 04_react_app_shell_routes.md | App Shell、Header、Sidebar、Routes |
| 5 | 05_react_auth_pages.md | 登入、註冊、2FA |
| 6 | 06_react_home_markets.md | 首頁、市場列表 |
| 7 | 07_react_personal_center.md | 個人中心完整頁面 |
| 8 | 08_react_assets_transfer.md | 資產、帳戶劃轉 |
| 9 | 09_react_deposit_withdraw.md | 充值、提現 |
| 10 | 10_react_spot_trading.md | 現貨交易頁 |
| 11 | 11_react_futures_trading.md | 合約交易頁 |
| 12 | 12_react_margin_trading.md | 槓桿交易頁 |
| 13 | 13_react_orders_positions.md | 訂單中心、倉位中心 |
| 14 | 14_react_api_security_notifications.md | API key、安全紀錄、通知 |
| 15 | 15_react_admin_console.md | 後台頁面 |
| 16 | 16_react_responsive_testing.md | 響應式、測試、交付檢查 |

---

## 最快可展示版本

```text
01_react_repo_scan.md
02_react_app_setup_rules.md
03_react_design_system_components.md
04_react_app_shell_routes.md
06_react_home_markets.md
07_react_personal_center.md
10_react_spot_trading.md
11_react_futures_trading.md
```

---

## 每次丟給 Codex 前加這句

```text
請只完成本文件任務。前端固定使用 React + TypeScript，資料可先用 mock service，不要實作其他文件內容，不要重構無關模組。
```


---

# FILE: 01_react_repo_scan.md

# 01 React Repo Scan：前端專案掃描，不改檔

## 任務

請掃描目前 repo 的前端結構，確認是否已經是 React 專案。  
本任務只做分析，不修改任何檔案。

---

## 限制

```text
不要修改任何檔案。
不要新增任何檔案。
不要安裝套件。
不要重構。
不要寫程式。
不要把專案改成 Next.js、Vue 或 Nuxt。
```

---

## 請輸出

| 項目 | 說明 |
|---|---|
| 是否 React | 確認是否 React |
| 是否 TypeScript | 確認 ts / tsx |
| 建置工具 | Vite、CRA、自定義等 |
| 路由 | React Router 或其他 |
| 入口檔 | main.tsx、App.tsx 等 |
| 目錄結構 | src、components、pages、services 等 |
| 樣式方案 | CSS、SCSS、Tailwind、CSS module |
| API 呼叫 | fetch、axios、client |
| 狀態管理 | hooks、context、redux、zustand |
| 共用元件 | 已有 Button、Table、Modal 等 |
| 測試 | vitest、jest、playwright 等 |
| 建議下一步 | 應先補哪個模組 |

---

## 驗收標準

```text
能知道 React 入口在哪。
能知道頁面放哪。
能知道元件放哪。
能知道 API service 放哪。
能知道是否能直接做 Vite React TypeScript。
沒有修改任何檔案。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```


---

# FILE: 02_react_app_setup_rules.md

# 02 React App Setup Rules：React 專案規則與目錄約定

## 任務

建立 React + TypeScript 前端工程規則文件與目錄約定。  
如果專案已有 docs 目錄，請放入 docs；否則建立 docs。

---

## 技術固定

```text
使用 React。
使用 TypeScript。
優先 Vite。
不要改成 Next.js。
不要使用 Vue / Nuxt。
不要新增大型狀態管理，除非 repo 已經使用。
不要新增大型 UI 套件，除非使用者確認。
```

---

## 建議目錄

```text
src/
  app/
  routes/
  pages/
  components/
  features/
  services/
  mocks/
  hooks/
  utils/
  styles/
  types/
```

---

## 功能分層

| 層 | 說明 |
|---|---|
| pages | 路由頁面 |
| components | 共用 UI |
| features | 交易所業務元件 |
| services | API client / mock service |
| hooks | React hooks |
| utils | 格式化、脫敏、計算 |
| types | TypeScript 型別 |
| mocks | mock data |

---

## 必要規則

```text
頁面不得直接寫死大量 mock，mock 放 services 或 mocks。
資產、價格、數量統一用格式化工具。
API key、Email、手機、地址必須脫敏。
交易頁先用 mock，不接真實 WebSocket。
高危操作必須有 ConfirmDialog 或 SecurityVerifyModal。
每個頁面要有 loading、empty、error 狀態。
```

---

## 需建立文件

| 文件 | 內容 |
|---|---|
| docs/FRONTEND_RULES.md | React 前端規則 |
| docs/ROUTES.md | 路由規劃 |
| docs/COMPONENTS.md | 共用元件規劃 |
| docs/MOCK_API.md | mock service 規則 |

---

## 驗收標準

```text
建立 React 前端規則文件。
建立路由與元件規劃。
明確禁止改成 Next.js / Vue。
明確要求 mock service。
明確要求 loading / empty / error。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```


---

# FILE: 03_react_design_system_components.md

# 03 React Design System：共用元件與格式化工具

## 任務

建立 React 交易所前端共用元件與工具骨架。  
優先沿用現有 UI；沒有就建立簡潔元件。

---

## 共用元件

| 元件 | 用途 |
|---|---|
| AppLayout | 全站框架 |
| Header | 主導航 |
| Sidebar | 側邊欄 |
| PageHeader | 頁面標題 |
| Card | 資訊卡片 |
| DataTable | 表格 |
| Tabs | 分頁 |
| Modal | 彈窗 |
| ConfirmDialog | 高危操作確認 |
| Toast | 操作提示 |
| Badge | 狀態標籤 |
| LoadingState | 載入狀態 |
| EmptyState | 空狀態 |
| ErrorState | 錯誤狀態 |

---

## 交易所專用元件

| 元件 | 用途 |
|---|---|
| AmountText | 金額格式 |
| PriceText | 價格格式 |
| PnlText | 盈虧格式 |
| KycStatusTag | KYC 狀態 |
| SecurityLevel | 安全等級 |
| RiskRatioBar | 槓桿風險率 |
| MarketPairSelector | 交易對選擇 |
| KlinePanel | K 線容器 |
| OrderBook | 深度 |
| TradeTape | 最新成交 |
| OrderEntryPanel | 下單面板 |
| PositionTable | 倉位表 |
| ApiKeyTable | API key 表 |
| NotificationList | 通知列表 |

---

## 工具函式

| 工具 | 說明 |
|---|---|
| formatPrice | 價格 |
| formatAmount | 數量 |
| formatCurrency | 金額 |
| formatPercent | 百分比 |
| formatTime | 時間 |
| maskEmail | Email 脫敏 |
| maskPhone | 手機脫敏 |
| maskApiKey | API key 脫敏 |
| maskAddress | 地址脫敏 |

---

## 不做範圍

```text
不要接真實行情。
不要實作完整 K 線圖表。
不要新增大型 UI 套件。
不要重構全站樣式。
```

---

## 驗收標準

```text
共用元件有基本骨架。
交易所專用元件有基本骨架。
格式化工具可用。
脫敏工具可用。
元件可被後續頁面 import。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```


---

# FILE: 04_react_app_shell_routes.md

# 04 React App Shell：主框架、Header、Sidebar、Routes

## 任務

建立 React App 主框架與路由。  
使用 React Router 或 repo 現有路由方案。

---

## 主路由

| 頁面 | 路由 |
|---|---|
| 首頁 | / |
| 市場 | /markets |
| 現貨 | /spot/:symbol |
| 合約 | /futures/:symbol |
| 槓桿 | /margin/:symbol |
| 資產 | /assets |
| 訂單 | /orders |
| 倉位 | /positions |
| 個人中心 | /account |
| 後台 | /admin |

---

## Header

```text
Logo
Markets
Spot
Futures
Margin
Assets
Orders
Account / Login
```

---

## Account Sidebar

```text
Overview
Security
KYC
Assets
Transfer
Deposit
Withdraw
API Keys
Notifications
Login History
Security Logs
Preferences
```

---

## Admin Sidebar

```text
Dashboard
Users
Assets
Wallet
Spot
Futures
Margin
Risk
Market Makers
Insurance Fund
Reconciliation
Operation Logs
Settings
```

---

## Auth 狀態

| 狀態 | 顯示 |
|---|---|
| 未登入 | Login / Register |
| 已登入 | Assets / Orders / Account |
| Admin | Admin entrance |
| 無權限 | Forbidden |
| 載入中 | Skeleton |

---

## 不做範圍

```text
不要實作完整 auth。
不要實作完整頁面內容。
不要改後端。
```

---

## 驗收標準

```text
React routes 可正常切換。
主 Header 可顯示。
Account sidebar 可顯示。
Admin sidebar 可顯示。
主要路由都有 placeholder。
未登入與已登入狀態有基礎 UI 或 TODO。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```


---

# FILE: 05_react_auth_pages.md

# 05 React Auth Pages：登入、註冊、2FA

## 任務

建立 React 認證頁面與安全驗證彈窗。  
可以在開發期使用 mock auth service；OL 前必須改接真實 auth API。

---

## 頁面

| 頁面 | 路由 |
|---|---|
| 登入 | /login |
| 註冊 | /register |
| 忘記密碼 | /forgot-password |
| 重置密碼 | /reset-password |
| 2FA 驗證 | /two-factor |

---

## 登入欄位

```text
Email / 手機
密碼
記住我
登入按鈕
忘記密碼
註冊入口
```

---

## 註冊欄位

```text
Email / 手機
驗證碼
密碼
確認密碼
邀請碼，可選
同意條款
```

---

## SecurityVerifyModal

用途：

```text
修改密碼
建立 API key
刪除 API key
新增提現地址
提現
關閉 2FA
```

驗證方式：

```text
Email code
SMS code
Google Authenticator
```

---

## 不做範圍

```text
不要實作真實 auth 後端。
不要保存明文密碼。
不要把 token 寫死。
```

---

## 驗收標準

```text
登入、註冊、忘記密碼、2FA 頁可訪問。
表單有基本驗證。
錯誤狀態可顯示。
SecurityVerifyModal 可復用。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```


---

# FILE: 06_react_home_markets.md

# 06 React Home / Markets：首頁與市場列表

## 任務

建立 React 首頁與市場列表頁。  
可以在開發期使用 mock market service；OL 前必須改接真實 market API / WebSocket。

---

## 首頁區塊

```text
Hero
熱門交易對
現貨市場
合約市場
產品入口
新手入口
Footer
```

---

## 市場頁

Tabs：

```text
Spot
Futures
Margin
Favorites
```

市場表格欄位：

```text
Symbol
Last Price
24h Change
24h High
24h Low
24h Volume
Action
```

---

## 初始 mock symbols

```text
BTC/USDT
ETH/USDT
SOL/USDT
BTCUSDT Perp
ETHUSDT Perp
SOLUSDT Perp
```

---

## 操作

```text
搜尋 symbol
切換市場類型
收藏，可 local state
點 Trade 跳轉交易頁
```

---

## 驗收標準

```text
首頁可訪問。
市場頁可訪問。
Spot / Futures / Margin 可切換。
表格有 loading、empty、error。
點擊 Trade 跳轉到 /spot/:symbol 或 /futures/:symbol。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```


---

# FILE: 07_react_personal_center.md

# 07 React Personal Center：個人中心完整頁面

## 任務

建立 React 個人中心完整頁面群。  
本任務只做畫面、元件、mock service 與 API hook，不做真實交易後端。

---

## 路由

| 頁面 | 路由 |
|---|---|
| 帳戶總覽 | /account |
| 安全中心 | /account/security |
| KYC | /account/kyc |
| 資產摘要 | /account/assets |
| 帳戶劃轉 | /account/transfer |
| API Key | /account/api-keys |
| 通知中心 | /account/notifications |
| 登入紀錄 | /account/login-history |
| 安全操作紀錄 | /account/security-logs |
| 偏好設定 | /account/preferences |

---

## 帳戶總覽卡片

| 卡片 | 內容 |
|---|---|
| Profile | UID、Email、手機、註冊時間 |
| KYC | 狀態、等級、限制 |
| Security | 安全等級、2FA、白名單 |
| Asset | 總估值、現貨、合約、槓桿 |
| Risk | 未完成安全項、風險提示 |

---

## 安全中心

```text
Login Password
Google Authenticator
Email Verification
SMS Verification
Withdrawal Whitelist
Device Management
Security Activity
```

---

## KYC 頁

狀態：

```text
未認證
審核中
已認證
拒絕
需補件
```

需顯示：

```text
KYC 等級
可提現額度
合約權限
槓桿權限
拒絕原因
提交入口 placeholder
```

---

## 資產摘要 Tabs

```text
Spot Account
Futures Account
Margin Account
```

欄位：

```text
Asset
Available
Frozen
Margin Used
Debt
Interest
Equity
Estimated Value
Action
```

---

## API Key 頁

```text
建立 API key
顯示一次 secret
read / spot trade / futures trade / margin trade
IP 白名單
停用
刪除
最近使用
```

---

## 不做範圍

```text
不要實作真實資產變更。
不要實作真實 KYC 上傳。
不要實作真實 API key 後端。
使用 mock service。
```

---

## 驗收標準

```text
所有個人中心子路由可訪問。
Sidebar 可切換。
帳戶總覽顯示 UID、KYC、安全、資產摘要。
安全中心顯示各安全項狀態。
KYC 顯示狀態與限制。
資產摘要分現貨、合約、槓桿。
API Key 頁有建立、停用、刪除 UI。
所有頁面有 loading、empty、error。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```


---

# FILE: 08_react_assets_transfer.md

# 08 React Assets / Transfer：資產總覽與帳戶劃轉

## 任務

建立 React 資產總覽與帳戶劃轉頁。

---

## 路由

| 頁面 | 路由 |
|---|---|
| 資產總覽 | /assets |
| 現貨帳戶 | /assets/spot |
| 合約帳戶 | /assets/futures |
| 槓桿帳戶 | /assets/margin |
| 帳戶劃轉 | /assets/transfer |
| 資產流水 | /assets/history |

---

## 資產總覽

```text
Total Equity
Spot Value
Futures Equity
Margin Equity
Account Tabs
Asset Table
Recent Asset History
```

---

## 帳戶表格欄位

現貨：

```text
Asset
Available
Frozen
Total
Estimated Value
Action
```

合約：

```text
Asset
Wallet Balance
Available
Margin Used
Unrealized PnL
Equity
Action
```

槓桿：

```text
Asset
Available
Borrowed
Interest
Net Asset
Risk Ratio
Action
```

---

## 劃轉表單

```text
From Account
To Account
Asset
Amount
Available
Max Button
Submit
```

支援：

```text
Spot → Futures
Futures → Spot
Spot → Margin
Margin → Spot
```

---

## 驗收標準

```text
資產總覽可訪問。
三種帳戶可切換。
劃轉表單可輸入。
不可選相同來源與目標。
超過可用餘額顯示錯誤。
提交後顯示成功 / 失敗狀態。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```


---

# FILE: 09_react_deposit_withdraw.md

# 09 React Deposit / Withdraw：充值與提現頁

## 任務

建立 React 充值、提現、充值紀錄、提現紀錄頁。  
可以在開發期使用 mock wallet service，不接真實鏈；OL 前必須改接真實 wallet / chain API。

---

## 路由

| 頁面 | 路由 |
|---|---|
| 充值 | /assets/deposit |
| 提現 | /assets/withdraw |
| 充值紀錄 | /assets/deposit/history |
| 提現紀錄 | /assets/withdraw/history |
| 提現地址 | /assets/withdraw/addresses |

---

## 充值頁欄位

```text
Asset
Network
Deposit Address
QR Code Placeholder
Copy Address
Minimum Deposit
Confirmations Required
Recent Deposits
```

---

## 提現頁欄位

```text
Asset
Network
Withdraw Address
Amount
Available
Fee
Receive Amount
Security Verification
Submit
```

---

## 提現地址管理

```text
新增地址
地址白名單
地址標籤
刪除地址
啟用 / 停用白名單
```

---

## 紀錄欄位

充值：

```text
Time
Asset
Network
Amount
Tx Hash
Confirmations
Status
```

提現：

```text
Time
Asset
Network
Amount
Fee
Receive Amount
Address
Tx Hash
Status
```

---

## 驗收標準

```text
充值頁可選 asset 與 network。
地址可 copy。
提現頁可填地址與數量。
fee 與 receive amount 可顯示。
新增地址使用安全驗證 modal。
紀錄表有 loading、empty、error。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```


---

# FILE: 10_react_spot_trading.md

# 10 React Spot Trading Page：現貨交易頁

## 任務

建立 React 現貨交易頁。  
可以在開發期使用 mock order book、mock trades、mock orders；OL 前必須改接真實 order book / trade feed / orders API，不可把 mock 當正式流程。

---

## 路由

```text
/spot/BTCUSDT
/spot/ETHUSDT
/spot/SOLUSDT
```

---

## 版面

```text
Header
  ↓
Symbol Header: Last Price / 24h Change / 24h Volume
  ↓
Kline + OrderBook + OrderPanel
  ↓
Open Orders
  ↓
Order History / Trade History
```

---

## 元件

| 元件 | 說明 |
|---|---|
| MarketPairSelector | 切換交易對 |
| KlinePanel | K 線容器 |
| OrderBook | 深度 |
| TradeTape | 最新成交 |
| SpotOrderPanel | 買入 / 賣出 |
| OpenOrdersTable | 當前委託 |
| OrderHistoryTable | 歷史訂單 |
| TradeHistoryTable | 成交紀錄 |

---

## 下單面板

```text
Buy / Sell
Limit / Market
Price
Amount
Total
Available
Fee Estimate
Submit
```

---

## 驗收標準

```text
現貨頁可訪問。
可切換 symbol。
K 線、深度、成交可顯示。
買入 / 賣出面板可輸入。
表單有基本驗證。
訂單表有 loading、empty、error。
未登入顯示 Login to Trade。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```


---

# FILE: 11_react_futures_trading.md

# 11 React Futures Trading Page：U 本位永續合約頁

## 任務

建立 React 合約交易頁。  
可以在開發期使用 mock 合約行情、mock 倉位、mock 訂單；OL 前必須改接真實行情、倉位與訂單 API，不能把 mock 強平或 mock 行情當正式流程。

---

## 路由

```text
/futures/BTCUSDT
/futures/ETHUSDT
/futures/SOLUSDT
```

---

## 版面

```text
Header
  ↓
Contract Header: Last / Mark Price / Index Price / Funding Rate / Countdown
  ↓
Kline + OrderBook + FuturesOrderPanel
  ↓
Positions
  ↓
Open Orders
  ↓
Trade History / Funding History
```

---

## 元件

| 元件 | 說明 |
|---|---|
| ContractSelector | 合約選擇 |
| MarkPriceInfo | 標記價格 |
| IndexPriceInfo | 指數價格 |
| FundingRateInfo | 資金費率 |
| FuturesOrderPanel | 開多 / 開空 / 平倉 |
| LeverageSelector | 槓桿 |
| MarginModeSelector | 逐倉 / 全倉 |
| PositionTable | 倉位 |
| FuturesOrdersTable | 合約委託 |
| FundingHistoryTable | 資金費紀錄 |

---

## 下單面板

```text
Open Long
Open Short
Close
Order Type
Price
Size
Leverage
Margin Mode
Cost
Estimated Liq Price
Fee Estimate
Submit
```

---

## 倉位欄位

```text
Symbol
Side
Size
Entry Price
Mark Price
Liq Price
Margin
Leverage
Unrealized PnL
ROE
Action
```

---

## 驗收標準

```text
合約頁可訪問。
顯示 mark price、index price、funding rate。
可選槓桿。
可選保證金模式。
下單面板有開多、開空、平倉。
倉位表顯示 PnL 與強平價。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```


---

# FILE: 12_react_margin_trading.md

# 12 React Margin Trading Page：槓桿交易頁

## 任務

建立 React 槓桿交易頁。  
可以在開發期使用 mock 槓桿資產、負債、利息、風險率；OL 前必須改接真實資產與風險 API。

---

## 路由

```text
/margin/BTCUSDT
/margin/ETHUSDT
```

---

## 版面

```text
Margin Account Summary
  ↓
Borrow / Repay Panel
  ↓
Kline + OrderBook + Trade Panel
  ↓
Open Orders
  ↓
Borrow History / Repay History / Interest History
```

---

## 元件

| 元件 | 說明 |
|---|---|
| MarginSummaryCard | 資產、負債、利息 |
| RiskRatioBar | 風險率 |
| BorrowPanel | 借幣 |
| RepayPanel | 還款 |
| MarginOrderPanel | 槓桿買賣 |
| BorrowHistoryTable | 借款紀錄 |
| RepayHistoryTable | 還款紀錄 |
| InterestHistoryTable | 利息紀錄 |

---

## 風險狀態

```text
Safe
Warning
Danger
Liquidation
```

---

## 驗收標準

```text
槓桿頁可訪問。
顯示資產、負債、利息、風險率。
借幣與還款表單可顯示。
槓桿交易面板可顯示。
歷史紀錄表可顯示。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```


---

# FILE: 13_react_orders_positions.md

# 13 React Orders / Positions：訂單中心與倉位中心

## 任務

建立 React 訂單中心與倉位中心頁。

---

## 路由

| 頁面 | 路由 |
|---|---|
| 訂單中心 | /orders |
| 現貨訂單 | /orders/spot |
| 合約訂單 | /orders/futures |
| 槓桿訂單 | /orders/margin |
| 成交紀錄 | /orders/trades |
| 倉位中心 | /positions |
| 強平紀錄 | /positions/liquidations |
| 資金費紀錄 | /positions/funding |

---

## 訂單 Tabs

```text
Spot Open Orders
Spot Order History
Spot Trade History
Futures Open Orders
Futures Order History
Futures Trade History
Margin Orders
```

---

## 倉位欄位

```text
Symbol
Side
Size
Entry Price
Mark Price
Liq Price
Margin
Leverage
Unrealized PnL
Realized PnL
ROE
Action
```

---

## 操作

```text
撤單
批量撤單
平倉
追加保證金
查看詳情
```

---

## 驗收標準

```text
訂單中心可訪問。
可以切換現貨、合約、槓桿。
表格支援搜尋、狀態篩選、時間篩選。
倉位中心可顯示合約倉位。
強平紀錄可顯示。
所有表格有 loading、empty、error。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```


---

# FILE: 14_react_api_security_notifications.md

# 14 React API Key / Security / Notifications：API、安全紀錄、通知

## 任務

建立 React API Key 管理、安全操作紀錄、登入紀錄、通知中心。

---

## 路由

| 頁面 | 路由 |
|---|---|
| API Key | /account/api-keys |
| 安全紀錄 | /account/security-logs |
| 登入紀錄 | /account/login-history |
| 通知中心 | /account/notifications |

---

## API Key 表格

```text
Name
Key Preview
Permissions
IP Whitelist
Created At
Last Used
Status
Action
```

---

## 建立 API Key Modal

```text
Name
Read
Spot Trade
Futures Trade
Margin Trade
IP Whitelist
Security Verification
```

成功後：

```text
顯示 API key
顯示 secret，一次性
提醒保存
```

---

## 安全紀錄

```text
Time
Action
IP
Device
Result
Details
```

---

## 通知分類

```text
All
Security
Deposit
Withdraw
Trade
Futures
Margin
System
```

---

## 驗收標準

```text
API key 列表可顯示。
建立 API key modal 可打開。
secret 只在建立成功畫面顯示。
可以停用 / 刪除 API key UI。
安全紀錄可顯示。
登入紀錄可顯示。
通知可分類與標記已讀。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```


---

# FILE: 15_react_admin_console.md

# 15 React Admin Console：後台前端頁面

## 任務

建立 React 後台管理頁面骨架。  
使用 mock admin service。

---

## 路由

| 頁面 | 路由 |
|---|---|
| Dashboard | /admin |
| Users | /admin/users |
| Assets | /admin/assets |
| Wallet | /admin/wallet |
| Spot | /admin/spot |
| Futures | /admin/futures |
| Margin | /admin/margin |
| Risk | /admin/risk |
| Market Makers | /admin/market-makers |
| Insurance Fund | /admin/insurance-fund |
| Reconciliation | /admin/reconciliation |
| Operation Logs | /admin/operation-logs |
| Settings | /admin/settings |

---

## Dashboard 卡片

```text
Total Users
Daily Volume
Open Withdrawals
Risk Alerts
Active Market Makers
Insurance Fund Balance
Reconciliation Status
System Status
```

---

## 管理頁面

| 頁面 | 需要內容 |
|---|---|
| Users | 用戶表、凍結、解凍、重置 2FA |
| Assets | 資產表、流水入口 |
| Wallet | 充值、提現、審核 |
| Spot | 交易對、訂單、成交、費率 |
| Futures | 合約、倉位、強平、資金費率 |
| Margin | 借貸、負債、利息、強平 |
| Risk | kill switch、規則、黑白名單 |
| Market Makers | 做市商、API、績效 |
| Reconciliation | 對帳結果與差異 |
| Logs | 操作日誌 |

---

## 高危操作

必須使用 ConfirmDialog：

```text
凍結用戶
解凍用戶
審核提現
拒絕提現
暫停交易
僅減倉
停用 API key
停用做市商
停止內部 MM
```

---

## 驗收標準

```text
/admin 可訪問。
後台 sidebar 可切換。
Dashboard 卡片可顯示。
主要後台頁有表格。
Risk 頁有 kill switch UI。
高危操作有 ConfirmDialog。
所有表格有 loading、empty、error。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```


---

# FILE: 16_react_responsive_testing.md

# 16 React Responsive / Testing：響應式、狀態、測試、交付

## 任務

建立 React 前端響應式規則、狀態處理、測試與交付檢查。

---

## 全站狀態

```text
loading
empty
error
success
disabled
unauthorized
forbidden
maintenance
```

---

## 格式化

| 類型 | 規則 |
|---|---|
| 價格 | 根據 symbol 精度 |
| 數量 | 根據 asset 精度 |
| 金額 | 千分位與 USDT |
| PnL | 正負清楚 |
| 百分比 | 兩位小數 |
| 時間 | YYYY-MM-DD HH:mm:ss |
| API Key | 脫敏 |
| 地址 | 前後截斷 |

---

## 響應式

| 裝置 | 規則 |
|---|---|
| Desktop | 完整交易布局 |
| Tablet | 表格與 order book 壓縮 |
| Mobile | Tabs 堆疊 |
| Admin | Desktop 優先 |

Mobile 交易頁：

```text
Symbol Header
Tabs: Kline / OrderBook / Trades
Order Panel
Positions / Orders
```

---

## 必測路由

```text
/
 /markets
/login
/register
/account
/account/security
/account/api-keys
/assets
/assets/transfer
/assets/deposit
/assets/withdraw
/spot/BTCUSDT
/futures/BTCUSDT
/margin/BTCUSDT
/orders
/positions
/admin
/admin/risk
```

---

## 交付標準

```text
主要頁面可訪問。
沒有明顯 console error。
沒有 TypeScript error。
主要表單有驗證。
主要表格有 loading / empty / error。
敏感資訊已脫敏。
高危操作有二次確認。
```

---

## 驗收標準

```text
有前端 QA checklist。
主要頁面符合基本響應式。
格式化工具被主要頁面使用。
敏感資訊有脫敏。
可執行 lint / typecheck / test 或有 TODO。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```
