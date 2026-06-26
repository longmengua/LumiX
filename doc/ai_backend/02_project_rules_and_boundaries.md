# 02 Project Rules：全專案工程規則與邊界

## 任務

建立交易所 MVP 的工程規則文件與必要的基礎約束。  
本 repo 的前端固定為 web/ React + TypeScript + Vite，`web/src/` 只給前端使用。  
後端固定為 Java 21 + Spring Boot 3，未來程式碼放在 `server/`。
正式交易核心目標為 C++ Core，未來程式碼預計放在 `core/` 或 `matching-core/`。

---

## 產品範圍

第一版交易所 MVP 包含：

```text
現貨交易
U 本位永續合約
槓桿交易
充值提現
Open API
外部做市商
內部做市商
後台營運
風控系統
強平系統
對帳系統
```

---

## 技術棧邊界

```text
前端：web/ React + TypeScript + Vite
後端：Java 21 + Spring Boot 3
後端目錄：server/
交易核心：C++ Core
交易核心目錄：core/ 或 matching-core/
Build tool：Gradle 優先
Database：PostgreSQL
Cache：Redis
Event bus：Kafka / Redpanda / RabbitMQ，可先 stub
```

```text
一般 CRUD 可使用 Spring Data JPA。
Java 業務後端的交易核心接入層、資產帳本、訂單、對帳優先使用 jOOQ / MyBatis / JDBC Template。
不要把整個後端完全交給 JPA 自動管理。
不要把後端改成 Node / Fastify / Prisma / TypeScript backend。
```

---

## 不在本任務實作的內容

```text
不要實作撮合。
不要實作強平。
不要實作錢包掃鏈。
不要實作前端完整頁面。
不要實作做市策略。
不要改動大量無關檔案。
不要建立 server/ 程式碼。
不要建立 core/ 或 matching-core/ 程式碼。
```

---

## 必須建立的規則文件

| 文件 | 內容 |
|---|---|
| docs/PROJECT_RULES.md | 全專案工程規則 |
| docs/DOMAIN_BOUNDARIES.md | 模組邊界 |
| docs/SECURITY_RULES.md | 安全與敏感操作規則 |
| docs/ASSET_RULES.md | 資產與帳本規則 |
| docs/CODEX_TASK_RULES.md | 後續 Codex 任務規則 |

---

## 核心工程規則

```text
web/src/ 只屬於 React 前端，不得移動或改造成後端。
後端固定為 Java 21 + Spring Boot 3，未來程式碼只放 server/。
正式交易核心目標為 C++ Core，Java Order Service 只保留 interface / skeleton / TODO，透過 MatchingEngineClient、gRPC 或 event bus 與 C++ Core 通訊。
所有資產變動必須通過帳本服務。
任何業務模組不得直接修改餘額。
現貨、合約、槓桿帳戶必須隔離。
所有敏感操作必須預留二次驗證。
所有後台敏感操作必須寫 operation log。
所有 API key 操作必須寫 security log。
所有充值 callback、提現、成交結算必須具備冪等設計。
合約必須使用指數價格與標記價格。
合約必須有強平與保險基金設計。
槓桿必須有借款、還款、利息、風險率與強平設計。
Open API 必須有簽名、timestamp、IP 白名單與 rate limit。
內部做市商與外部做市商都必須走 Open API。
Matching Engine 先以 Java `MatchingEngineClient` interface 方式存在，正式目標為 C++ Core，未來可透過 gRPC 或 event bus 接入。
C++ Core 不得直接修改 `user_balance`、`ledger_journal`、`wallet`、`withdraw`、`admin adjustment`。
Settlement / Ledger Service 負責資產結算與資產流水。
所有 C++ Core 輸出事件都必須包含 `event_id`、`sequence`、`symbol`、`timestamp`，並支援重放、對帳、補償。
所有高風險邏輯必須標記 `TODO: requires high-reasoning review before production use`。
```

---

## 模組邊界

| 模組 | 負責 |
|---|---|
| User | 用戶、登入、安全、KYC 狀態 |
| Admin | 後台、RBAC、審批、操作日誌 |
| Asset | 統一帳本、資產、流水、劃轉 |
| Wallet | 充值、提現、掃鏈、callback |
| Spot | 現貨訂單、撮合接入、成交、結算 |
| Futures | 合約訂單、倉位、保證金、資金費率 |
| Margin | 借幣、還款、利息、負債、風險率 |
| Market Data | 深度、成交、ticker、K 線 |
| Price Index | 外部價格、指數價、標記價 |
| Risk | 限額、黑白名單、風控規則 |
| Liquidation | 合約與槓桿強平 |
| Open API | API key、簽名、限流、IP 白名單 |
| Market Maker | 做市商帳號、報表、內部 MM |
| Reconcile | 對帳、快照、補償 |

---

## 驗收標準

```text
已建立工程規則文件。
文件中清楚說明資產不得直接修改。
文件中清楚說明現貨、合約、槓桿帳戶隔離。
文件中清楚說明敏感操作、API key、後台操作的安全要求。
文件中清楚說明 Java 21 + Spring Boot 3 + server/。
文件中清楚說明 Java 業務後端 + C++ Core 分工。
沒有實作無關業務功能。
```

---

## Codex 回覆格式

請依照以下格式回覆：

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

風險與注意事項：
- ...
```
