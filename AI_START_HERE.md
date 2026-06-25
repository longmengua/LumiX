# AI_START_HERE.md

> 用途：這是本專案給 Codex / AI 工程助手的主控文件。  
> 之後使用者只要輸入「繼續開工」，AI 就必須依照本文件與 `AI_PROGRESS.md` 自動判斷下一步、執行任務、回報進度，並在每個 Phase 結束時停下等待人工審查。

---

## 0. 專案狀態

目前專案名稱：LumiX  
產品目標：交易所 MVP，包含現貨、U 本位永續合約、槓桿交易、充值提現、Open API、外部做市商、內部做市商、後台、風控、強平、對帳。  
前端固定技術：root React + TypeScript + Vite，`src/` 僅作前端。  
後端固定技術：Java 21 + Spring Boot 3，未來放在 `server/`。  
正式交易核心目標為 C++ Core，未來程式碼預計放在 `core/` 或 `matching-core/`。
目前 repo 內已有需求文件，但尚未建立正式前端 / 後端程式骨架。

目前已存在文件：

```text
doc/exchange_mvp_plan.md
doc/ai_backend/*.md
doc/ai_frontend/*.md
server/（未建立，後端未來放置位置）
```

重要文件：

```text
doc/ai_backend/00_README_投餵順序.md
doc/ai_frontend/00_REACT_FE_README_投餵順序.md
doc/ai_backend/01_repo_scan.md
doc/ai_frontend/01_react_repo_scan.md
```

---

## 1. 使用者互動規則

當使用者輸入：

```text
繼續開工
```

AI 必須執行以下流程：

```text
1. 讀取 AI_START_HERE.md
2. 讀取 AI_PROGRESS.md
3. 檢查目前 Phase 狀態
4. 如果上一個 Phase 尚未人工審查通過，停止並要求使用者審查
5. 如果可以繼續，選擇下一個未完成任務
6. 讀取該任務需要的 doc 文件
7. 判斷任務等級：Level A / B / C / D
8. 根據等級決定可實作範圍
9. 執行任務
10. 跑可用的檢查指令
11. 更新 AI_PROGRESS.md
12. 回報完成內容、修改檔案、測試結果、TODO、風險、下一步
```

AI 不可以在沒有使用者允許的情況下一次完成多個 Phase。  
每個 Phase 結束後必須停止，等待使用者人工審查。

---

## 2. 模型分級規則

### Level A：mini medium 可直接做

適合：

```text
前端頁面
路由
表格
表單
mock service
API hook
CRUD 骨架
文件
Layout
狀態展示
通知列表
後台查詢頁
```

允許：

```text
直接實作
可以新增 React 元件
可以新增 mock data
可以新增頁面與路由
可以新增基礎文件
```

禁止：

```text
不得實作真實資產扣帳
不得實作真實撮合
不得實作真實強平
不得實作真實錢包掃鏈
```

---

### Level B：mini high 或 medium + 嚴格限制

適合：

```text
schema
service interface
API route 骨架
帳戶劃轉骨架
充值提現狀態流
Open API 簽名骨架
風控規則骨架
對帳 job 骨架
```

允許：

```text
建立資料結構
建立 interface
建立 route
建立 service stub
建立 TODO
```

禁止：

```text
不得完成資產真實扣帳邏輯
不得完成成交結算
不得完成強平公式
不得完成槓桿風險率核心邏輯
不得處理真實私鑰或鏈上出帳
```

---

### Level C：需要大模型 high 先設計

適合：

```text
資產帳本真實邏輯
現貨成交結算
撮合一致性
合約倉位
保證金
PnL
強平
保險基金
槓桿借貸風險率
資金費率
對帳修復策略
```

AI 可以做：

```text
設計草案
interface
stub
TODO
測試案例
風險清單
```

AI 不可以做：

```text
不可自行完成生產級核心交易邏輯
不可宣稱已可上線
不可繞過人工審查
```

---

### Level D：需要人工審查

適合：

```text
私鑰與錢包
提現
用戶資產
後台人工調帳
合約強平
槓桿壞帳
API withdraw 權限
正式上線前安全審查
```

AI 只能：

```text
列審查清單
列風險
列必改項
列測試項
等待使用者確認
```

---

## 3. 全專案硬性規則

以下規則任何情況都不能違反：

```text
1. root `src/` 只屬於 React 前端，不得移動或改造成後端。
2. 後端固定為 Java 21 + Spring Boot 3，未來程式碼只放 `server/`。
3. Build tool 以 Gradle 優先，正式後端不得回退為 Node / Fastify / Prisma / TypeScript backend。
4. Database 使用 PostgreSQL，Cache 使用 Redis。
5. Event bus 可使用 Kafka / Redpanda / RabbitMQ，MVP 允許先 stub。
6. 一般 CRUD 可使用 Spring Data JPA。
7. 交易核心、資產帳本、訂單、對帳優先使用 jOOQ / MyBatis / JDBC Template，不要完全依賴 JPA 自動管理。
8. 不得直接修改用戶資產餘額。
9. 所有資產變動必須通過 ledger service 或預留 ledger interface。
10. 現貨、合約、槓桿帳戶必須隔離。
11. 所有充值、提現、成交結算、資金費率、強平流程必須預留冪等設計。
12. API key secret 只允許建立時顯示一次，不可明文持久化。
13. API withdraw 權限預設不可開。
14. 內部做市商與外部做市商都必須走 Open API。
15. 後台高危操作必須有 RBAC、二次確認、operation log。
16. 合約強平必須使用標記價格，不可直接使用最新成交價。
17. Matching Engine 先定 Java `MatchingEngineClient` interface，正式目標為 C++ Core，Java Order Service 僅作接入層，未來透過 gRPC 或 event bus 與 C++ Core 通訊。
18. C++ Core 不得直接修改 `user_balance`、`ledger_journal`、`wallet`、`withdraw`、`admin adjustment`。
19. Settlement / Ledger Service 負責資產結算與資產流水，所有 C++ Core 輸出事件必須包含 `event_id`、`sequence`、`symbol`、`timestamp`，並支援重放、對帳、補償。
20. 所有高風險邏輯必須標記 `TODO: requires high-reasoning review before production use`。
```

---

## 4. 開工總策略

由於目前 repo 只有文件，沒有正式程式碼，第一階段先建立 React 專案骨架與前端畫面。  
後端技術棧先校準為 Java 21 + Spring Boot 3 + `server/`，交易核心正式目標為 C++ Core。  
Phase 9-12 只建立 Java 業務後端的骨架、interface、stub 與 TODO，不直接實作高風險邏輯，也不建立 C++ production 程式碼。

優先順序：

```text
Phase 0：Repo 掃描與工作追蹤
Phase 1：React + TypeScript 專案骨架
Phase 2：Design System 與 App Shell
Phase 3：登入 / 首頁 / 市場列表
Phase 4：個人中心
Phase 5：資產、劃轉、充值、提現畫面
Phase 6：現貨、合約、槓桿交易頁
Phase 7：訂單、倉位、API Key、通知
Phase 8：後台前端頁面
Phase 9：建立 `server/` Spring Boot 後端骨架、帳戶與帳本 interface
Phase 10：Wallet、Market Data、Spot、Open API Java stub
Phase 11：Futures、Liquidation、Margin Java skeleton
Phase 12：風控、對帳、測試、上線檢查
```

目前本次文件更新只做交易核心 C++ 校準，不進入 Phase 3。

---

## 5. Phase 計畫

### Phase 0：Repo 掃描與進度初始化

模型等級：Level A  
建議 reasoning：medium

任務：

```text
P0-01 掃描 repo 結構
P0-02 確認目前沒有 package.json 與 src
P0-03 建立或更新 AI_PROGRESS.md
P0-04 回報建議開工方向
```

參考文件：

```text
doc/ai_backend/01_repo_scan.md
doc/ai_frontend/01_react_repo_scan.md
```

完成條件：

```text
AI_PROGRESS.md 存在
目前 repo 狀態已記錄
下一個任務明確
```

Phase 結束後：不需要人工審查，可繼續 Phase 1。

---

### Phase 1：React + TypeScript 專案骨架

模型等級：Level A  
建議 reasoning：medium

任務：

```text
P1-01 建立 React + TypeScript 專案骨架
P1-02 使用 Vite 或現有 repo 合適方案
P1-03 建立 src 目錄
P1-04 建立 package.json
P1-05 建立基礎 scripts：dev、build、lint 或 typecheck
P1-06 建立基本入口頁
```

參考文件：

```text
doc/ai_frontend/02_react_app_setup_rules.md
doc/ai_frontend/03_react_design_system_components.md
```

限制：

```text
不要接交易 API
不要實作交易邏輯
不要新增大型 UI 套件，除非必要
```

完成條件：

```text
npm / pnpm install 可執行
dev server 可啟動
build 或 typecheck 可通過
```

Phase 結束後：必須等待使用者人工審查。

---

### Phase 2：Design System 與 App Shell

模型等級：Level A  
建議 reasoning：medium

任務：

```text
P2-01 建立 Layout
P2-02 建立 Header
P2-03 建立 Sidebar
P2-04 建立基礎路由
P2-05 建立共用元件：Card、Table、Tabs、Modal、Toast、Loading、Empty、Error
P2-06 建立交易所格式化工具：價格、數量、金額、百分比、時間、脫敏
```

參考文件：

```text
doc/ai_frontend/03_react_design_system_components.md
doc/ai_frontend/04_react_app_shell_routes.md
```

完成條件：

```text
主要路由可訪問
Layout 正常
共用元件可被頁面引用
```

Phase 結束後：必須等待使用者人工審查。

---

### Phase 3：登入、首頁、市場列表

模型等級：Level A  
建議 reasoning：medium

任務：

```text
P3-01 登入頁
P3-02 註冊頁
P3-03 忘記密碼頁
P3-04 2FA 驗證頁
P3-05 首頁行情
P3-06 市場列表
P3-07 mock auth service
P3-08 mock market service
```

參考文件：

```text
doc/ai_frontend/05_react_auth_pages.md
doc/ai_frontend/06_react_home_markets.md
```

完成條件：

```text
登入 / 註冊 / 首頁 / 市場列表可訪問
市場列表有 Spot / Futures tabs
資料可使用 mock
```

Phase 結束後：必須等待使用者人工審查。

---

### Phase 4：個人中心

模型等級：Level A  
建議 reasoning：medium

任務：

```text
P4-01 個人中心 Layout
P4-02 帳戶總覽
P4-03 安全中心
P4-04 KYC 狀態頁
P4-05 資產摘要入口
P4-06 API Key 管理入口
P4-07 通知中心入口
P4-08 登入紀錄與安全操作紀錄
P4-09 mock account service
```

參考文件：

```text
doc/ai_frontend/07_react_personal_center.md
doc/ai_backend/04_personal_center.md
```

完成條件：

```text
個人中心所有子路由可訪問
安全 / KYC / 資產 / API key / 通知都有頁面骨架
敏感資訊有脫敏
```

Phase 結束後：必須等待使用者人工審查。

---

### Phase 5：資產、劃轉、充值、提現畫面

模型等級：Level A  
建議 reasoning：medium

任務：

```text
P5-01 資產總覽
P5-02 現貨帳戶頁
P5-03 合約帳戶頁
P5-04 槓桿帳戶頁
P5-05 帳戶劃轉頁
P5-06 充值頁
P5-07 提現頁
P5-08 充值紀錄
P5-09 提現紀錄
P5-10 提現地址頁
```

參考文件：

```text
doc/ai_frontend/08_react_assets_transfer.md
doc/ai_frontend/09_react_deposit_withdraw.md
doc/ai_backend/05_unified_account_ledger.md
doc/ai_backend/06_wallet_deposit_withdraw.md
```

限制：

```text
只做前端與 mock API
不得實作真實資產扣帳
不得實作真實鏈上出帳
```

完成條件：

```text
資產三帳戶可展示
劃轉表單可展示與驗證
充值 / 提現頁可展示
```

Phase 結束後：必須等待使用者人工審查。

---

### Phase 6：交易頁

模型等級：Level A  
建議 reasoning：medium

任務：

```text
P6-01 現貨交易頁
P6-02 合約交易頁
P6-03 槓桿交易頁
P6-04 mock order book
P6-05 mock trades
P6-06 mock open orders
P6-07 mock positions
P6-08 mock margin risk ratio
```

參考文件：

```text
doc/ai_frontend/10_react_spot_trading.md
doc/ai_frontend/11_react_futures_trading.md
doc/ai_frontend/12_react_margin_trading.md
```

限制：

```text
不得實作真實撮合
不得實作真實 PnL
不得實作真實強平
不得實作真實槓桿風險率
```

完成條件：

```text
現貨交易頁可展示 K 線容器、深度、下單、委託
合約交易頁可展示標記價、指數價、資金費率、倉位
槓桿交易頁可展示借幣、還款、風險率
```

Phase 結束後：必須等待使用者人工審查。

---

### Phase 7：訂單、倉位、API Key、通知

模型等級：Level A  
建議 reasoning：medium

任務：

```text
P7-01 訂單中心
P7-02 倉位中心
P7-03 強平紀錄
P7-04 資金費率紀錄
P7-05 API Key 管理完整頁
P7-06 安全紀錄
P7-07 通知中心
```

參考文件：

```text
doc/ai_frontend/13_react_orders_positions.md
doc/ai_frontend/14_react_api_security_notifications.md
```

完成條件：

```text
訂單與倉位查詢頁可展示
API key 頁支援建立、停用、刪除 UI
通知中心有分類與已讀狀態
```

Phase 結束後：必須等待使用者人工審查。

---

### Phase 8：後台前端頁面

模型等級：Level A  
建議 reasoning：medium

任務：

```text
P8-01 Admin Layout
P8-02 Dashboard
P8-03 Users
P8-04 Assets
P8-05 Wallet
P8-06 Spot
P8-07 Futures
P8-08 Margin
P8-09 Risk
P8-10 Market Makers
P8-11 Insurance Fund
P8-12 Reconciliation
P8-13 Operation Logs
```

參考文件：

```text
doc/ai_frontend/15_react_admin_console.md
doc/ai_backend/14_admin_console.md
```

完成條件：

```text
/admin 可訪問
各後台頁面有表格與 mock data
高危操作使用 ConfirmDialog
```

Phase 結束後：必須等待使用者人工審查。

---

### Phase 9：建立 `server/` Spring Boot 後端骨架、帳戶與帳本 interface

模型等級：Level B / C  
建議 reasoning：high

任務：

```text
P9-01 建立 `server/` 後端目錄骨架
P9-02 建立 docs 規則文件
P9-03 建立帳戶模型 interface
P9-04 建立 ledger service interface
P9-05 建立 account transfer service stub
P9-06 建立 idempotency 設計 stub
```

參考文件：

```text
doc/ai_backend/02_project_rules_and_boundaries.md
doc/ai_backend/05_unified_account_ledger.md
```

限制：

```text
不得建立 Node / Fastify / Prisma / TypeScript backend
不得完成真實資產扣帳生產邏輯
不得跳過 ledger service
所有核心邏輯標記 TODO: requires high-reasoning review before production use
```

完成條件：

```text
帳戶與帳本 interface 存在
沒有直接修改資產餘額的業務邏輯
```

Phase 結束後：必須等待使用者人工審查。

---

### Phase 10：Wallet、Market Data、Spot、Open API Java stub

模型等級：Level B  
建議 reasoning：high

任務：

```text
P10-01 wallet service stub
P10-02 deposit / withdraw 狀態模型
P10-03 market data service stub
P10-04 price index service stub
P10-05 spot order service stub
P10-06 open api route stub
```

參考文件：

```text
doc/ai_backend/06_wallet_deposit_withdraw.md
doc/ai_backend/07_market_data_price_index.md
doc/ai_backend/08_spot_trading.md
doc/ai_backend/12_open_api.md
```

限制：

```text
不得建立 Node / Fastify / Prisma / TypeScript backend
不得處理真實私鑰
不得真實鏈上廣播
不得真實撮合
不得真實成交結算
```

完成條件：

```text
API stub 可被前端 mock / adapter 替換
核心高風險邏輯都有 TODO
```

Phase 結束後：必須等待使用者人工審查。

---

### Phase 11：Futures、Liquidation、Margin Java skeleton

模型等級：Level C  
建議 reasoning：high

任務：

```text
P11-01 futures service skeleton
P11-02 position model skeleton
P11-03 funding rate skeleton
P11-04 liquidation service skeleton
P11-05 insurance fund skeleton
P11-06 margin service skeleton
P11-07 borrow / repay / interest skeleton
```

參考文件：

```text
doc/ai_backend/09_futures_trading.md
doc/ai_backend/10_liquidation_insurance_fund.md
doc/ai_backend/11_margin_trading.md
```

限制：

```text
不得建立 Node / Fastify / Prisma / TypeScript backend
不得自行完成 PnL 公式
不得自行完成強平公式
不得自行完成保證金公式
不得自行完成槓桿風險率公式
所有核心計算標記 TODO: requires high-reasoning review before production use
```

完成條件：

```text
合約、強平、槓桿 skeleton 存在
所有高風險核心都標記需要審查
```

Phase 結束後：必須等待使用者人工審查。

---

### Phase 12：風控、對帳、測試、上線檢查

模型等級：Level B / C / D  
建議 reasoning：high

任務：

```text
P12-01 risk service skeleton
P12-02 kill switch config
P12-03 reconciliation job skeleton
P12-04 compensation task skeleton
P12-05 frontend testing checklist
P12-06 backend safety checklist
P12-07 go-live checklist
```

參考文件：

```text
doc/ai_backend/15_risk_engine.md
doc/ai_backend/16_reconciliation_jobs.md
doc/ai_backend/18_testing_and_go_live.md
doc/ai_frontend/16_react_responsive_testing.md
```

限制：

```text
不得建立 Node / Fastify / Prisma / TypeScript backend
不得自動修帳
不得自動調整資產
不得宣稱可正式上線
必須等待人工審查
```

完成條件：

```text
風控、對帳、補償、測試、上線檢查文件與骨架存在
```

Phase 結束後：必須等待使用者人工審查。

---

## 6. 每次回報格式

每次執行完任務，AI 必須用以下格式回報：

```text
本次 Phase：
本次任務：
任務等級：
是否需要人工審查：

完成摘要：
- ...

讀取文件：
- ...

修改檔案：
- ...

新增檔案：
- ...

測試 / 檢查：
- 指令：
- 結果：

未完成 TODO：
- ...

風險與注意事項：
- ...

AI_PROGRESS.md 更新：
- ...

下一步建議：
- ...
```

---

## 7. 人工審查規則

每個 Phase 結束後，AI 必須停止並輸出：

```text
Phase X 已完成，請人工審查。
若審查通過，請在 AI_PROGRESS.md 將該 Phase 的 review_status 改為 approved，或回覆「Phase X 審查通過，繼續開工」。
```

使用者可回覆：

```text
Phase X 審查通過，繼續開工
```

AI 收到後才可以進入下一個 Phase。

---

## 8. 禁止行為

AI 不得：

```text
一次跨多個 Phase 實作
未審查就進入下一個 Phase
重構無關檔案
新增大型套件而不說明
硬編私鑰、token、secret
明文保存 API secret
直接修改資產餘額
實作未審查的強平或保證金公式
宣稱系統可正式上線
自動修帳
```

---

## 9. 第一次啟動指令

使用者第一次可輸入：

```text
請閱讀 AI_START_HERE.md，初始化 AI_PROGRESS.md，然後執行 Phase 0。不要修改其他無關檔案。
```

之後只要輸入：

```text
繼續開工
```

AI 就必須依照本文件繼續。
