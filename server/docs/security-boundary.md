# Security Boundary

## 目的

本文件定義 LumiX 後端 security boundary skeleton。
Phase 13 只先建立 authentication、authorization、principal 與高風險操作分類，不實作登入、API key runtime、withdrawal signing 或 admin runtime。

## 職責切分

- `authentication`：確認「你是誰」
- `authorization`：確認「你可不可以做」
- `principal`：表示主體是 `USER`、`ADMIN` 或 `SERVICE`
- `apikey`：保留 API key / signature / IP whitelist / rate limit 的 boundary
- `admin`：保留管理端 guard boundary

## Principal model

```text
USER
ADMIN
SERVICE
```

## 高風險操作模型

```text
WITHDRAWAL_REQUEST
LEDGER_POSTING
ADMIN_ACTION
SETTLEMENT
RISK_OVERRIDE
```

這些操作都必須標記 `HUMAN_REVIEW_REQUIRED` 或 `production-gated`。

## 錯誤邊界

- security error 必須對齊 `ApiErrorResponse`
- 不得外洩 secret、signature payload、private key、SQL 或 stack trace
- withdrawal signing / broadcast 不在本題實作

## 文字圖

```text
+---------------+     +-------------------+     +---------------------+
| authentication| --> | authorization     | --> | security policy     |
+---------------+     +-------------------+     +---------------------+
         |                         |
         v                         v
   principal model           high-risk operation
```

## Phase 13 原則

- 只定義 boundary 與 policy skeleton。
- 不把 boundary skeleton 誤當成登入或權限系統已完成。
