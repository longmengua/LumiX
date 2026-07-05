# Phase 13 - 雙式帳本引擎

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
所有資金變動都要留下可稽核、不可任意改寫的帳本軌跡。

## 目標
實作可稽核的雙式帳本、冪等入帳與沖正。

## 為何需要這一章
所有資金變動都要留下可稽核、不可任意改寫的帳本軌跡。

## 依賴
- 前置章節：Phase 12。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
debit / credit 驗證、journal 不可變、冪等處理、併發控制、沖正模型、稽核軌跡。

## 非目標
reservation、撮合、結算、錢包 runtime。

## 必要產出
ledger 引擎、測試、對帳可讀資料。

## 驗收標準
只有平衡分錄可以入帳，不能出現負向亂改。

## 必要測試
posting、idempotency、reversal、concurrency。

## 可能影響的檔案與模組
ledger domain、repository、migration、服務邊界。

## 資料模型影響
journal 與 line 成為權威資料。

## API 影響
僅內部帳本介面。

## 安全影響
保存不可變財務歷史。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
凍結完成、現貨下單完成、撮合完成、結算完成。

## 下一階段交接
Phase 14 會用 journal 做餘額投影與比對。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_13_DOUBLE_ENTRY_LEDGER.md
本章目標：只做 Phase 13 - 雙式帳本引擎。
範圍：debit / credit 驗證、journal 不可變、冪等處理、併發控制、沖正模型、稽核軌跡。
不要做：reservation、撮合、結算、錢包 runtime。
產出：ledger 引擎、測試、對帳可讀資料。
測試：posting、idempotency、reversal、concurrency。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：凍結完成、現貨下單完成、撮合完成、結算完成。
輸出格式：Changed Files、Summary、What Phase 13 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
