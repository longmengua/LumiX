# 容器圖

```text
+--------------------------------------------------------------------------------+
| LumiX                                                                          |
|                                                                                |
|  +----------------+        +----------------+        +----------------------+   |
|  | Web App        | -----> | REST API       | -----> | Application Service |   |
|  | React/TS/Vite  |        | Spring Boot    |        | account/order/etc.  |   |
|  +----------------+        +----------------+        +----------+-----------+   |
|                                                               |                |
|                                                               v                |
|                                                   +----------------------+     |
|                                                   | Exchange Core        |     |
|                                                   | ledger/reserve/match |     |
|                                                   +----------+-----------+     |
|                                                               |                |
|       +----------------+        +----------------+            |                |
|       | Redis          | <----> | Workers        | <----------+                |
|       | cache/locks    |        | outbox/chain   |                             |
|       +----------------+        +----------------+                             |
|                                                               |                |
|                                                   +-----------v----------+     |
|                                                   | PostgreSQL           |     |
|                                                   | source of truth      |     |
|                                                   +----------------------+     |
+--------------------------------------------------------------------------------+
```

## 容器責任

- Web App：顯示交易、錢包、帳戶與管理畫面，不保存資金真相。
- REST API：驗證、授權、輸入檢查、idempotency、呼叫 application service。
- Application Service：交易邊界與流程協調。
- Exchange Core：帳本、凍結、撮合、結算與風控的核心規則。
- Workers：outbox delivery、chain listener、reconciliation、market data。
- PostgreSQL：資金與業務資料 source of truth。
- Redis：快取、短期鎖、rate limit，不是資金真相。
