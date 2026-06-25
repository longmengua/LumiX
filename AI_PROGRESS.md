# AI_PROGRESS.md

> 用途：記錄 AI / Codex 目前開工進度。  
> AI 每次執行任務後必須更新本文件。  
> 使用者每個 Phase 結束後需人工審查，審查通過後才能進入下一個 Phase。

---

## 0. Repo 狀態

專案名稱：LumiX  
目前狀態：前端已建立 React / TypeScript / Vite 專案骨架，後端技術棧校準為 Java 21 + Spring Boot 3，並已開始補齊交易核心 C++ 校準文件，等待 Phase 2 人工審查。  
目前觀察：

```text
- repo root 有 README.md
- repo root 有 doc/
- doc/ai_backend 存在
- doc/ai_frontend 存在
- repo root 有 package.json
- repo root 有 index.html
- repo root 有 vite.config.ts
- repo root 有 tsconfig*.json
- repo root 有 src/
- root `src/` 為 React 前端
- 前端入口已建立
- 後端未來放在 `server/`
```

---

## 1. Phase 狀態總覽

| Phase | 名稱 | status | review_status | 備註 |
|---:|---|---|---|---|
| 0 | Repo 掃描與進度初始化 | done | not_required | 已完成 |
| 1 | React + TypeScript 專案骨架 | done | pending | 需人工審查 |
| 2 | Design System 與 App Shell | done | pending | 需人工審查 |
| 3 | 登入、首頁、市場列表 | pending | pending | 需人工審查 |
| 4 | 個人中心 | pending | pending | 需人工審查 |
| 5 | 資產、劃轉、充值、提現畫面 | pending | pending | 需人工審查 |
| 6 | 現貨、合約、槓桿交易頁 | pending | pending | 需人工審查 |
| 7 | 訂單、倉位、API Key、通知 | pending | pending | 需人工審查 |
| 8 | 後台前端頁面 | pending | pending | 需人工審查 |
| 9 | 建立 `server/` Spring Boot 後端骨架、帳戶與帳本 interface | pending | pending | 高風險，需審查 |
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
current_phase: 2
current_task: P2-06
next_action: 等待使用者審查 Phase 2 與交易核心 C++ 校準文件，確認後再評估是否進入 Phase 3（登入、首頁、市場列表）
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
| P1-01 建立 React + TypeScript 專案骨架 | done | 已完成 | - |
| P1-02 使用 Vite 或合適方案 | done | 已完成 | - |
| P1-03 建立 src 目錄 | done | 已完成 | - |
| P1-04 建立 package.json | done | 已完成 | - |
| P1-05 建立 scripts | done | 已完成 | - |
| P1-06 建立基本入口頁 | done | 已完成 | - |

### Phase 2

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P2-01 建立 Layout | done | 已完成 | - |
| P2-02 建立 Header | done | 已完成 | - |
| P2-03 建立 Sidebar | done | 已完成 | - |
| P2-04 建立基礎路由 | done | 已完成 | - |
| P2-05 建立共用元件 | done | 已完成 | - |
| P2-06 建立格式化工具 | done | 已完成 | - |

### Phase 3

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P3-01 登入頁 | pending | - | - |
| P3-02 註冊頁 | pending | - | - |
| P3-03 忘記密碼頁 | pending | - | - |
| P3-04 2FA 驗證頁 | pending | - | - |
| P3-05 首頁行情 | pending | - | - |
| P3-06 市場列表 | pending | - | - |
| P3-07 mock auth service | pending | - | - |
| P3-08 mock market service | pending | - | - |

### Phase 4

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P4-01 個人中心 Layout | pending | - | - |
| P4-02 帳戶總覽 | pending | - | - |
| P4-03 安全中心 | pending | - | - |
| P4-04 KYC 狀態頁 | pending | - | - |
| P4-05 資產摘要入口 | pending | - | - |
| P4-06 API Key 管理入口 | pending | - | - |
| P4-07 通知中心入口 | pending | - | - |
| P4-08 登入紀錄與安全操作紀錄 | pending | - | - |
| P4-09 mock account service | pending | - | - |

### Phase 5

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P5-01 資產總覽 | pending | - | - |
| P5-02 現貨帳戶頁 | pending | - | - |
| P5-03 合約帳戶頁 | pending | - | - |
| P5-04 槓桿帳戶頁 | pending | - | - |
| P5-05 帳戶劃轉頁 | pending | - | - |
| P5-06 充值頁 | pending | - | - |
| P5-07 提現頁 | pending | - | - |
| P5-08 充值紀錄 | pending | - | - |
| P5-09 提現紀錄 | pending | - | - |
| P5-10 提現地址頁 | pending | - | - |

### Phase 6

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P6-01 現貨交易頁 | pending | - | - |
| P6-02 合約交易頁 | pending | - | - |
| P6-03 槓桿交易頁 | pending | - | - |
| P6-04 mock order book | pending | - | - |
| P6-05 mock trades | pending | - | - |
| P6-06 mock open orders | pending | - | - |
| P6-07 mock positions | pending | - | - |
| P6-08 mock margin risk ratio | pending | - | - |

### Phase 7

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P7-01 訂單中心 | pending | - | - |
| P7-02 倉位中心 | pending | - | - |
| P7-03 強平紀錄 | pending | - | - |
| P7-04 資金費率紀錄 | pending | - | - |
| P7-05 API Key 管理完整頁 | pending | - | - |
| P7-06 安全紀錄 | pending | - | - |
| P7-07 通知中心 | pending | - | - |

### Phase 8

| 任務 | status | 結果 | 備註 |
|---|---|---|---|
| P8-01 Admin Layout | pending | - | - |
| P8-02 Dashboard | pending | - | - |
| P8-03 Users | pending | - | - |
| P8-04 Assets | pending | - | - |
| P8-05 Wallet | pending | - | - |
| P8-06 Spot | pending | - | - |
| P8-07 Futures | pending | - | - |
| P8-08 Margin | pending | - | - |
| P8-09 Risk | pending | - | - |
| P8-10 Market Makers | pending | - | - |
| P8-11 Insurance Fund | pending | - | - |
| P8-12 Reconciliation | pending | - | - |
| P8-13 Operation Logs | pending | - | - |

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
| - | - | - | - | - |

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
已完成後端技術棧校準文件更新。
Phase 3 尚未開始，且本次文件更新明確暫停進入 Phase 3。
```
