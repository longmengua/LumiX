# Phase 12 - 資料庫結構與遷移

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
先把帳本、凍結、訂單、入金、出金與對帳的資料骨架固定下來。

## 目標
建立正式資料庫結構，讓後面的資金流程有共同底座。

## 為何需要這一章
先把帳本、凍結、訂單、入金、出金與對帳的資料骨架固定下來。

## 依賴
- 前置章節：Phase 11。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
accounts、assets、asset networks、symbols、balances、ledger journals、ledger lines、reservations、orders、fills、deposits、withdrawals、reconciliation、admin audit、migration tests。

## 非目標
帳本 runtime、資金變動、凍結、撮合、結算、錢包鏈上 runtime。

## 必要產出
schema 文件、migration 檔、索引與約束、migration tests。

## 驗收標準
可從零建立資料庫，欄位與約束正確，遷移順序固定。

## 必要測試
zero-to-latest migration、constraint check、smoke test。

## 可能影響的檔案與模組
server/src/main/resources/db/migration/。

## 資料模型影響
建立正式持久化表與約束。

## API 影響
暫無。

## 安全影響
資料欄位、權限與稽核基礎。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
帳本完成、資金變動完成、凍結完成、撮合完成、結算完成、正式交易完成。

## 下一階段交接
Phase 13 會在這份 schema 上做雙式帳本。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_12_DATABASE_SCHEMA.md
本章目標：只做 Phase 12 - 資料庫結構與遷移。
範圍：accounts、assets、asset networks、symbols、balances、ledger journals、ledger lines、reservations、orders、fills、deposits、withdrawals、reconciliation、admin audit、migration tests。
不要做：帳本 runtime、資金變動、凍結、撮合、結算、錢包鏈上 runtime。
產出：schema 文件、migration 檔、索引與約束、migration tests。
測試：zero-to-latest migration、constraint check、smoke test。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：帳本完成、資金變動完成、凍結完成、撮合完成、結算完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 12 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
