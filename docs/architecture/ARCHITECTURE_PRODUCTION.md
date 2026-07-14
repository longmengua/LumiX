# 生產架構

LumiX 的 production architecture 建立在五個核心原則上。

## 原則

1. 帳本是資金真相，不是 balance cache。
2. 下單前先凍結，撮合後再結算。
3. 交易結果必須可以重放、重算、對帳。
4. 錢包出帳與簽名流程必須比一般 API 更嚴格。
5. 所有跨系統副作用都必須有 idempotency 與 outbox / audit trail。

## 高層分層

```text
+-------------------------------+
| User Experience               |
| web, admin, public API        |
+---------------+---------------+
                |
+---------------v---------------+
| Application Services          |
| auth, account, order, wallet  |
+---------------+---------------+
                |
+---------------v---------------+
| Exchange Core                 |
| ledger, reservation, matching |
| settlement, risk              |
+---------------+---------------+
                |
+---------------v---------------+
| Persistence & Messaging       |
| PostgreSQL, Redis, outbox     |
+---------------+---------------+
                |
+---------------v---------------+
| External Systems              |
| chain node, custody, email    |
+-------------------------------+
```

## 生產資料規則

```text
Request accepted != funds moved
Order accepted   != trade settled
Withdrawal asked != withdrawal signed
Deposit seen     != deposit credited
```

每個狀態都要有可查詢的中間狀態，不可用一個 boolean 欄位概括整條生命週期。
