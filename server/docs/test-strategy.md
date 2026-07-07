# Test Strategy

## 目的

本文件定義 LumiX 後端測試分層與執行策略。
Phase 13 先建立測試基礎與命名方式，讓後續高風險流程能在一致層級下被驗證。

## 測試分類

```text
unit test
boundary test
schema migration test
integration test
```

## 各類測試用途

- `unit test`：驗證單一 class 或 policy，盡量不依賴 Spring context。
- `boundary test`：驗證 module boundary、error boundary、DTO / validation boundary、persistence boundary、transaction boundary、security boundary。
- `schema migration test`：驗證 Flyway migration、欄位型別、約束與 PostgreSQL replay。
- `integration test`：驗證多個 boundary 串接後的整體行為，通常會啟動 Spring context。

## H2 與 PostgreSQL / Testcontainers

- H2 compatibility mode 適合 schema migration test 的快速回歸與欄位結構檢查。
- PostgreSQL replay 必須用於高風險流程、資料相容性與實際 DDL 行為驗證。
- 若未來需要更接近 production 的驗證，再評估 Testcontainers，但不能用來掩蓋 PostgreSQL replay 的必要性。

## 高風險流程

下列流程未來必須使用 integration test 或 PostgreSQL replay 驗證：

```text
ledger
reservation
withdrawal
settlement
idempotency
outbox
```

## 安全限制

- integration test 不依賴外部真實交易所、真實錢包或真實鏈上節點。
- 測試不得使用真實 private key、secret 或 API key。
- 若新增 test profile，必須避免影響 production config。

## 文字圖

```text
+------------------+     +-------------------+     +----------------------+
| unit / boundary  | --> | integration test  | --> | PostgreSQL replay    |
+------------------+     +-------------------+     +----------------------+
```

## Phase 13 原則

- 先定義測試分層，再補具體 integration case。
- 不把 smoke test 當成完整 runtime 驗證。
