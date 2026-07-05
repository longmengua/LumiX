# AI_PROGRESS.md

> 用途：記錄 AI / Codex 目前開工進度。  
> AI 每次執行任務後必須更新本文件。  
> 使用者每個 Phase 結束後需人工審查，審查通過後才能進入下一個 Phase。

---

## 0. Repo 狀態

專案名稱：LumiX  
目前狀態：前端已建立 web/ React / TypeScript / Vite 專案骨架，後端技術棧校準為 Java 21 + Spring Boot 3，交易核心 C++ 校準文件已審查通過，Phase 1、Phase 2、Phase 3 已人工審查通過；Phase 3.5 與 Phase 4 已完成並審查通過；Phase 5、Phase 6 已完成並審查通過；Phase 7 已完成並審查通過，訂單 / 倉位 / API Key / 通知前端頁面與開發期 adapter 已建立；Phase 7.5 i18n 多語系基礎建設已完成，等待人工審查；Phase 7.6 layout / responsive UI QA 已完成並審查通過；Phase 7.6-hotfix 首頁與全域 layout 實機修復已完成並審查通過；Phase 7.6-hotfix-v2 主內容寬度與 PageHeader collapse 修復已完成，已再調整為流式 responsive layout，等待人工審查；Phase 7.6-auth-i18n-cleanup 已完成並審查通過；Phase 7.6-auth-brand-cleanup 已完成並審查通過；Phase 7.6-auth-visual-panel 已完成並審查通過；Phase 7.6-flex-only-layout-refactor 已完成並審查通過；Phase 8 後台前端頁面已完成並審查通過。  
目前觀察：

```text
- repo root 有 README.md
- repo root 有 doc/
- doc/ai_backend 存在
- doc/ai_frontend 存在
- repo root 有 web/
- web/ 有 package.json
- web/ 有 index.html
- web/ 有 vite.config.ts
- web/ 有 tsconfig*.json
- web/ 有 src/
- `web/src/` 為 React 前端
- 前端入口已建立（web/）
- 後端未來放在 `server/`
```

---

## 1. Phase 狀態總覽

| Phase | 名稱 | status | review_status | 備註 |
|---:|---|---|---|---|
| 0 | Repo 掃描與進度初始化 | done | not_required | 已完成 |
| 1 | React + TypeScript 專案骨架 | done | approved | 已審查通過 |
| 2 | Design System 與 App Shell | done | approved | 已審查通過 |
| 3 | 登入、首頁、市場列表 | done | approved | 已審查通過 |
| 3.5 | Monorepo 目錄結構校準 | done | approved | 已審查通過 |
| 4 | 個人中心 | done | approved | 已完成，人工審查通過 |
| 5 | 資產、劃轉、充值、提現畫面 | done | approved | 已審查通過 |
| 6 | 現貨、合約、槓桿交易頁 | done | approved | 已審查通過 |
| 7 | 訂單、倉位、API Key、通知 | done | approved | 已審查通過 |
| 7.5 | i18n 多語系基礎建設 | done | pending | 已完成 web/ 前端基礎 i18n，等待人工審查 |
| 7.6-hotfix | 首頁與全域 layout 實機修復 | done | approved | 已針對首頁 hero、content 寬度與桌機顯示做實機修復，人工審查通過 |
| 7.6-hotfix-v2 | 主內容寬度與 PageHeader collapse 修復 | done | approved | 已針對 `/`、`/markets`、`/spot/BTC-USDT`、`/futures/BTC-USDT`、`/orders`、`/positions` 做寬度與排版修復，並改為流式 responsive layout，人工審查通過 |
| 7.6-auth-i18n-cleanup | Auth i18n 清理 | done | approved | 已完成 login / register / forgot password / 2FA / reset password auth 文案 i18n 與開發痕跡清除，人工審查通過 |
| 7.6-auth-brand-cleanup | Auth brand 清理 | done | approved | 已完成 Auth 文案產品化、Logo component 與 Header / Auth shell 品牌更新，人工審查通過 |
| 7.6-auth-visual-panel | Auth 視覺面板 | done | approved | 已完成 Auth 左側品牌視覺面板、動畫與 i18n 補強，人工審查通過 |
| 8 | 後台前端頁面 | done | approved | 已完成 admin console / mock data / confirm dialogs，人工審查通過 |
| 9 | 建立 `server/` Spring Boot 後端骨架、帳戶與帳本 interface | in_progress | pending | 高風險，進行中 |
| 10 | Wallet、Market Data、Spot、Open API Java stub | pending | pending | 高風險，需審查 |
| 11 | Futures、Liquidation、Margin Java skeleton | pending | pending | 高風險，需審查 |
| 12 | 風控、對帳、測試、上線檢查 | pending | pending | 高風險，需審查 |

狀態說明：

```text
pending：尚未開始
in_progress：進行中
done：AI 已完成
blocked：被阻擋
approved：使用者已審查通過
```

review_status 說明：

```text
not_required：不需要人工審查
pending：等待人工審查
approved：人工審查通過
changes_requested：需要修改
```

---

## 2. 當前任務

```text
current_phase: Phase 9
current_task: phase_9_server_spring_boot_account_ledger_foundation
next_action: 執行 Phase 9 後端骨架、帳戶與帳本 interface
```

---

## 3. 任務紀錄

### Phase 0

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P0-01 掃描 repo 結構 | done | 已確認 | - |
| P0-02 確認目前沒有 package.json 與 src | done | 已確認 | - |
| P0-03 建立或更新 AI_PROGRESS.md | done | 已更新 | - |
| P0-04 回報建議開工方向 | done | 已回報 | - |

### Phase 1

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P1-01 建立 React + TypeScript 專案骨架 | done | 已完成 | 已審查通過 |
| P1-02 使用 Vite 或合適方案 | done | 已完成 | 已審查通過 |
| P1-03 建立 src 目錄 | done | 已完成 | 已審查通過 |
| P1-04 建立 package.json | done | 已完成 | 已審查通過 |
| P1-05 建立 scripts | done | 已完成 | 已審查通過 |
| P1-06 建立基本入口頁 | done | 已完成 | 已審查通過 |

### Phase 2

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P2-01 建立 Layout | done | 已完成 | 已審查通過 |
| P2-02 建立 Header | done | 已完成 | 已審查通過 |
| P2-03 建立 Sidebar | done | 已完成 | 已審查通過 |
| P2-04 建立基礎路由 | done | 已完成 | 已審查通過 |
| P2-05 建立共用元件 | done | 已完成 | 已審查通過 |
| P2-06 建立格式化工具 | done | 已完成 | 已審查通過 |

### Phase 3

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P3-01 登入頁 | done | 已完成 | 已完成 |
| P3-02 註冊頁 | done | 已完成 | 已完成 |
| P3-03 忘記密碼頁 | done | 已完成 | 已完成 |
| P3-04 2FA 驗證頁 | done | 已完成 | 已完成 |
| P3-05 首頁行情 | done | 已完成 | 已完成 |
| P3-06 市場列表 | done | 已完成 | 已完成 |
| P3-07 auth service（開發期 mock） | done | 已完成 | 已完成 |
| P3-08 market service（開發期 mock） | done | 已完成 | 已完成 |

### Phase 3.5

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P3.5-01 將 root 前端移入 web/ | done | 已完成 | 已完成，待審查 |

### Phase 4

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P4-01 個人中心 Layout | done | 已完成 | 已完成 |
| P4-02 帳戶總覽 | done | 已完成 | 已完成 |
| P4-03 安全中心 | done | 已完成 | 已完成 |
| P4-04 KYC 狀態頁 | done | 已完成 | 已完成 |
| P4-05 資產摘要入口 | done | 已完成 | 已完成 |
| P4-06 API Key 管理入口 | done | 已完成 | 已完成 |
| P4-07 通知中心入口 | done | 已完成 | 已完成 |
| P4-08 登入紀錄與安全操作紀錄 | done | 已完成 | 已完成 |
| P4-09 account service（開發期 mock） | done | 已完成 | 已完成 |

### Phase 5

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P5-01 資產總覽 | done | 已完成 | Phase 5 第一個頁面群 |
| P5-02 現貨帳戶頁 | done | 已完成 | 已拆出 route-level 共用元件 |
| P5-03 合約帳戶頁 | done | 已完成 | 已拆出 route-level 共用元件 |
| P5-04 槓桿帳戶頁 | done | 已完成 | 已拆出 route-level 共用元件 |
| P5-05 帳戶劃轉頁 | done | 已完成 | 已拆出 route-level 共用元件 |
| P5-06 充值頁 | done | 已完成 | 開發期 adapter，含地址、QR placeholder、memo / tag、recent deposits |
| P5-07 提現頁 | done | 已完成 | 含 2FA、白名單、風控審核、手續費與到帳預估 |
| P5-08 充值紀錄 | done | 已完成 | 含 loading、empty、error |
| P5-09 提現紀錄 | done | 已完成 | 含 loading、empty、error |
| P5-10 提現地址頁 | done | 已完成 | 含白名單、風險標記、停用 / 刪除與安全驗證 modal |

### Phase 6

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P6-01 現貨交易頁 | done | 已完成 | Development adapter only，含 order book / tape / order form / open orders |
| P6-02 合約交易頁 | done | 已完成 | Development adapter only，含 positions / funding preview |
| P6-03 槓桿交易頁 | done | 已完成 | Development adapter only，含 borrow / risk snapshot |
| P6-04 order book integration | done | 已完成 | mock 前端 order book 區塊 |
| P6-05 trade feed integration | done | 已完成 | mock 前端 trade feed 區塊 |
| P6-06 open orders integration | done | 已完成 | mock 前端 open orders 區塊 |
| P6-07 positions integration | done | 已完成 | mock 前端 positions 區塊 |
| P6-08 margin risk ratio integration | done | 已完成 | mock 前端 risk ratio 區塊 |

### Phase 7

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P7-01 訂單中心 | done | 已完成 | Development adapter only，含 open / history / fills |
| P7-02 倉位中心 | done | 已完成 | Development adapter only，含 open / liquidation / funding |
| P7-03 強平紀錄 | done | 已完成 | 開發期 liquidation adapter snapshot |
| P7-04 資金費率紀錄 | done | 已完成 | 開發期 funding adapter snapshot |
| P7-05 API Key 管理完整頁 | done | 已完成 | development adapter only，secret 只在建立時顯示一次 |
| P7-06 安全紀錄 | done | 已完成 | 通知 / 安全事件以本地 snapshot 呈現 |
| P7-07 通知中心 | done | 已完成 | 開發期通知中心，含 unread / filter / read toggle |

### Phase 7.5

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P7.5-01 建立 `web/src/i18n` 架構 | done | 已完成 | 含 `I18nProvider`、`useI18n`、`t(key)`、`locale / setLocale`、localStorage 持久化 |
| P7.5-02 Header 語言切換 UI | done | 已完成 | 可在 `zh-TW` 與 `en-US` 間切換 |
| P7.5-03 sidebar / navigation 接入 i18n | done | 已完成 | 含 account / admin / assets / orders / positions / trading |
| P7.5-04 page header 與 common state 文案接入 | done | 已完成 | 含 loading / empty / error 常見文字 |
| P7.5-05 更新 AI_PROGRESS | done | 已完成 | Phase 7.5 完成紀錄，等待人工審查 |

### Phase 7.6

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P7.6-01 全域 layout / header / sidebar 響應式修正 | done | 已完成 | 1440 / 1024 / 390 基準下避免重疊與橫向爆版 |
| P7.6-02 table / grid / form / tabs 響應式修正 | done | 已完成 | 主要表格區支援窄螢幕水平捲動或堆疊 |
| P7.6-03 trading / assets / account / wallet UI QA | done | 已完成 | 針對中英文字長差異調整 |
| P7.6-04 build / typecheck 驗證 | done | 已完成 | `npm run build` 與 `npm run typecheck` 通過 |

### Phase 7.6-hotfix

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P7.6H-01 首頁 hero / CTA / highlight cards 實機修復 | done | 已完成 | 解決 1440px 左右首頁壓窄與重疊問題 |
| P7.6H-02 AppLayout / content 寬度與置中修復 | done | 已完成 | 主內容改為可讀寬度並置中 |
| P7.6H-03 Header / PageHeader / Card 寬度保護 | done | 已完成 | 避免標題與 actions 被擠壓 |
| P7.6H-04 build / typecheck 驗證 | done | 已完成 | `npm run build` 與 `npm run typecheck` 通過 |

### Phase 7.6-hotfix-v2

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P7.6HV2-01 主內容寬度與 PageHeader collapse 修復 | done | 已完成 | 已針對 `/`、`/markets`、`/spot/BTC-USDT`、`/futures/BTC-USDT`、`/orders`、`/positions` 做寬度與排版修復，並改為流式 responsive layout |
| P7.6HV2-02 build / typecheck 驗證 | done | 已完成 | `npm run build` 與 `npm run typecheck` 通過 |

### Phase 7.6-auth-i18n-cleanup

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P7.6AIC-01 Auth shell 與頁面文案 i18n 化 | done | 已完成 | login / register / forgot password / 2FA / reset password 已接入 i18n |
| P7.6AIC-02 開發痕跡文案清理 | done | 已完成 | 移除 / 替換對外可見的 Phase 3、demo-ready、Root React + TypeScript + Vite 等文案 |
| P7.6AIC-03 build / typecheck 驗證 | done | 已完成 | `npm run build` 與 `npm run typecheck` 通過 |

### Phase 7.6-auth-brand-cleanup

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P7.6ABC-01 Logo component 建立 | done | 已完成 | 新增可重用 `Logo` component，支援 full / mark 與 size |
| P7.6ABC-02 Header / Auth shell 品牌更新 | done | 已完成 | Header 左上角與 Auth hero 改用 Logo component |
| P7.6ABC-03 dev notice env gate | done | 已完成 | 開發提示預設不顯示，僅在 `VITE_SHOW_DEV_NOTICES=true` 時顯示且走 i18n |
| P7.6ABC-04 build / typecheck 驗證 | done | 已完成 | `npm run build` 與 `npm run typecheck` 通過 |

### Phase 7.6-flex-only-layout-refactor

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P7.6FLEX-01 CSS Grid 最終清零 | done | 已完成 | 已將剩餘 `display:grid` / `grid-template` / `grid-column` / `fr` 清除，等待人工審查 |
| P7.6FLEX-02 build / typecheck 驗證 | done | 已完成 | `npm run build` 與 `npm run typecheck` 通過 |

### Phase 8

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P8-01 Admin Layout | done | 已完成 | 已掛載 /admin shell、sidebar 與路由 |
| P8-02 Dashboard | done | 已完成 | 已加入 dashboard cards 與 summary mock data |
| P8-03 Users | done | 已完成 | 已加入使用者表格、凍結 / 解凍與 2FA reset confirm dialogs |
| P8-04 Assets | done | 已完成 | 已加入資產餘額表格 |
| P8-05 Wallet | done | 已完成 | 已加入充值 / 提現審核表格與 confirm dialogs |
| P8-06 Spot | done | 已完成 | 已加入交易對狀態表格與 pause / resume confirm dialogs |
| P8-07 Futures | done | 已完成 | 已加入合約狀態表格與 mode 切換 confirm dialogs |
| P8-08 Margin | done | 已完成 | 已加入借貸與風險率表格 |
| P8-09 Risk | done | 已完成 | 已加入 kill switch / withdraw pause / reduce only UI |
| P8-10 Market Makers | done | 已完成 | 已加入做市商狀態表格與 disable / enable confirm dialogs |
| P8-11 Insurance Fund | done | 已完成 | 已加入保險基金表格 |
| P8-12 Reconciliation | done | 已完成 | 已加入對帳結果表格 |
| P8-13 Operation Logs | done | 已完成 | 已加入操作紀錄表格 |

### Phase 9：建立 `server/` Spring Boot 後端骨架、帳戶與帳本 interface

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P9-01 建立 `server/` 後端目錄骨架 | pending | - | 高風險前置 |
| P9-02 建立 docs 規則文件 | pending | - | - |
| P9-03 建立帳戶模型 interface | pending | - | 不可直接改餘額 |
| P9-04 建立 ledger service interface | pending | - | 核心 |
| P9-05 建立 account transfer service stub | pending | - | - |
| P9-06 建立 idempotency 設計 stub | pending | - | - |

### Phase 10：Wallet、Market Data、Spot、Open API Java stub

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P10-01 wallet service stub | pending | - | 不處理私鑰 / 鏈上出帳 |
| P10-02 deposit / withdraw 狀態模型 | pending | - | - |
| P10-03 market data service stub | pending | - | - |
| P10-04 price index service stub | pending | - | - |
| P10-05 spot order service stub | pending | - | 不做撮合 |
| P10-06 open api route stub | pending | - | withdraw 預設不開 |

### Phase 11：Futures、Liquidation、Margin Java skeleton

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P11-01 futures service skeleton | pending | - | 高風險 / Java skeleton |
| P11-02 position model skeleton | pending | - | 高風險 |
| P11-03 funding rate skeleton | pending | - | 高風險 |
| P11-04 liquidation service skeleton | pending | - | 極高風險 |
| P11-05 insurance fund skeleton | pending | - | 高風險 |
| P11-06 margin service skeleton | pending | - | 高風險 |
| P11-07 borrow / repay / interest skeleton | pending | - | 高風險 |

### Phase 12

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P12-01 risk service skeleton | pending | - | 高風險 |
| P12-02 kill switch config | pending | - | 高風險 |
| P12-03 reconciliation job skeleton | pending | - | 高風險 / Java job |
| P12-04 compensation task skeleton | pending | - | 不可自動修帳 |
| P12-05 frontend testing checklist | pending | - | - |
| P12-06 backend safety checklist | pending | - | - |
| P12-07 go-live checklist | pending | - | 不可宣稱已可上線 |

---

## 4. 人工審查紀錄

| Phase | 審查人 | 結果 | 時間 | 備註 |
|---:|---|---|---|---|
| 4 | Codex | 通過 | 2026-06-26 | Phase 4 UI / mock 範圍完成；build 通過；typecheck 通過；仍屬開發期 mock / adapter，不代表 OL 真實後端完成；OL 前需接 `server/` Java 真實 API |
| 5 | Codex | 通過 | 2026-06-29 | Phase 5 資產 / 劃轉 / 充值 / 提現 UI 與開發期 adapter 完成；build 通過；typecheck 通過；OL 前仍需接 `server/` Java 真實 API |
| 6 | Codex | 通過 | 2026-06-29 | Phase 6 現貨 / 合約 / 槓桿交易頁與開發期 adapter 完成；build 通過；typecheck 通過；OL 前仍需接 `server/` Java API、C++ Core event stream 與真正結算流程 |
| 1 | Codex | 通過 | 2026-06-25 | Phase 1 React + TypeScript 專案骨架已通過人工審查 |
| 2 | Codex | 通過 | 2026-06-25 | Phase 2 Design System 與 App Shell 已通過人工審查 |
| 3 | Codex | 通過 | 2026-06-25 | Phase 3 登入、首頁、市場列表已通過人工審查 |
| 7 | Codex | 通過 | 2026-06-25 | Phase 7 訂單、倉位、API Key、通知前端頁面已通過人工審查 |
| 8 | Codex | 通過 | 2026-07-05 | Phase 8 後台前端頁面完成；admin console / mock data / confirm dialogs 已審查通過，等待下一階段後端骨架與帳本介面 |

---

## 5. 重要 TODO

```text
TODO: Phase 1 完成後確認 React 專案可正常啟動。
TODO: Phase 4 完成後確認個人中心是否符合營運需求。
TODO: Phase 6 完成後確認交易頁資訊密度與產品體驗。
TODO: Phase 9 之後所有資產相關邏輯需高 reasoning 審查。
TODO: Phase 11 強平、保證金、PnL、風險率不得未審查上線。
TODO: Phase 1 開始前先完成使用者人工確認，避免跨 Phase。
```

---

## 6. 最後一次 AI 回報

```text
已完成交易核心 C++ 校準文件更新，且 Phase 2 已審查通過。
Phase 3、Phase 3.5 與 Phase 4 已完成；Phase 5 已完成，充值與提現前端頁面已建立，已完成 cleanup 並修正轉帳提示文案，等待人工審查。
```
