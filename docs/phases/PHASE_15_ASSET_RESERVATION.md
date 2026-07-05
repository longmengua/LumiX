# Phase 15 - 資產預留與凍結引擎

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
交易與出金都需要先把資金鎖住，不能直接亂扣。

## 目標
實作 reserve、release、commit、rollback、locked balance 與 available balance。

## 為何需要這一章
交易與出金都需要先把資金鎖住，不能直接亂扣。

## 依賴
- 前置章節：Phase 12～14。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
部分成交、取消釋放、冪等 reservation 事件、卡住 reservation 偵測。

## 非目標
撮合、結算、錢包 runtime。

## 必要產出
reservation 引擎、測試、狀態機說明。

## 驗收標準
available + locked = total，reserve 流程可以重試且不亂。

## 必要測試
reserve / release / commit / rollback、partial fill、cancel、stuck reservation。

## 可能影響的檔案與模組
reservation domain、ledger hooks、order service 邊界。

## 資料模型影響
reservation 狀態表。

## API 影響
內部 reservation 邊界。

## 安全影響
避免負餘額與未授權釋放。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
正式現貨下單完成、撮合完成、結算完成、正式交易完成。

## 下一階段交接
Phase 16 會在這個引擎上編排下單流程。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_15_ASSET_RESERVATION.md
本章目標：只做 Phase 15 - 資產預留與凍結引擎。
範圍：部分成交、取消釋放、冪等 reservation 事件、卡住 reservation 偵測。
不要做：撮合、結算、錢包 runtime。
產出：reservation 引擎、測試、狀態機說明。
測試：reserve / release / commit / rollback、partial fill、cancel、stuck reservation。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：正式現貨下單完成、撮合完成、結算完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 15 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
