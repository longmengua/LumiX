# 02 Project Rules：全專案工程規則與邊界

## 任務

建立交易所 MVP 的工程規則文件與必要的基礎約束。  
如果 repo 已有 docs 或 architecture 目錄，請放在既有位置；否則建立 docs 目錄。

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

## 不在本任務實作的內容

```text
不要實作撮合。
不要實作強平。
不要實作錢包掃鏈。
不要實作前端完整頁面。
不要實作做市策略。
不要改動大量無關檔案。
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
```

---

## 模組邊界

| 模組 | 負責 |
|---|---|
| User | 用戶、登入、安全、KYC 狀態 |
| Admin | 後台、RBAC、審批、操作日誌 |
| Asset | 統一帳戶、資產、流水、劃轉 |
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
