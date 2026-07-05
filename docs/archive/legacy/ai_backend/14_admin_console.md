# 14 Admin Console：後台營運系統

## 任務

建立交易所後台營運模組。  
後台必須能支援用戶、資產、錢包、現貨、合約、槓桿、風控、做市商、對帳與事故處理。
後端實作預期為 Java 21 + Spring Boot 3；後台查詢與 CRUD 可優先使用 JPA，資產 / 訂單 / 對帳查詢可走 jOOQ / MyBatis / JDBC Template。

---

## 後台模組

| 模組 | 功能 |
|---|---|
| 用戶管理 | 查用戶、KYC、凍結、解凍、安全重置 |
| 資產管理 | 查現貨、合約、槓桿帳戶與流水 |
| 錢包管理 | 充值、提現、審核、補單 |
| 現貨管理 | 訂單、成交、交易對、費率 |
| 合約管理 | 倉位、強平、資金費率、槓桿 |
| 槓桿管理 | 借貸、負債、利息、強平 |
| 風控管理 | 規則、黑白名單、限額 |
| 做市商管理 | 帳號、API key、限流、績效 |
| 保險基金 | 餘額、流水、穿倉 |
| 對帳報表 | 每日對帳結果 |
| 操作日誌 | 所有後台操作 |

---

## 事故處理能力

| 能力 | 說明 |
|---|---|
| 暫停現貨交易對 | spot pause |
| 暫停合約交易對 | futures pause |
| 合約僅減倉 | reduce only |
| 暫停槓桿借幣 | margin borrow pause |
| 暫停提現 | withdraw pause |
| 凍結用戶 | user freeze |
| 凍結資產 | asset freeze |
| 停用 API key | api key disable |
| 停用做市商 | MM disable |
| 停止內部 MM | internal MM stop |
| 查對帳異常 | reconciliation mismatch |

---

## 後台 API

| API | 說明 |
|---|---|
| 查用戶 | user list |
| 凍結 / 解凍用戶 | user status |
| 查資產 | balances |
| 查資產流水 | ledger |
| 查充值 | deposits |
| 查提現 | withdraws |
| 審核提現 | approve / reject |
| 查現貨訂單 | spot orders |
| 查合約倉位 | futures positions |
| 查槓桿負債 | margin debts |
| 查強平紀錄 | liquidations |
| 查做市商 | market makers |
| 查對帳結果 | reconciliation |
| 執行 kill switch | emergency |
| 查操作日誌 | operation logs |

---

## 安全要求

```text
後台所有寫操作必須驗證權限。
後台所有敏感操作必須寫 operation log。
高危操作需要二次確認或審批。
不得在後台直接改餘額。
人工調帳必須走審批與帳本服務。
```

---

## 不做範圍

```text
不要在後台直接實作交易邏輯。
不要在後台直接改資料庫資產。
不要跳過 RBAC。
```

---

## 驗收標準

```text
後台可查用戶、資產、流水。
後台可查充值與提現。
後台可審核提現。
後台可查現貨訂單與成交。
後台可查合約倉位與強平。
後台可查槓桿借貸與負債。
後台可查做市商。
後台可執行基礎 kill switch。
所有後台寫操作都有 operation log。
後台不得直接改餘額或繞過帳本。
TODO: requires high-reasoning review before production use
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
