# Phase 31 - 保證金借貸系統

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
借貸是保證金交易的延伸能力。

## 目標
建立借款、還款、利息、抵押品估值、借款上限與強制還款。

## 為何需要這一章
借貸是保證金交易的延伸能力。

## 依賴
- 前置章節：Phase 13～15、26。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
borrow、repay、interest accrual、collateral valuation、margin level、bad debt handling。

## 非目標
現貨撮合、錢包出金。

## 必要產出
lending ledger、利息模型、測試。

## 驗收標準
借貸與利息可以追蹤且可對帳。

## 必要測試
borrow、repay、interest、forced repayment。

## 可能影響的檔案與模組
lending、risk、ledger 擴充。

## 資料模型影響
借貸帳本與利息記錄。

## API 影響
借貸查詢與操作。

## 安全影響
避免超借與壞帳。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
借貸上線完成、正式交易完成。

## 下一階段交接
Phase 32 會做跨域對帳與補償。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_31_MARGIN_LENDING.md
本章目標：只做 Phase 31 - 保證金借貸系統。
範圍：borrow、repay、interest accrual、collateral valuation、margin level、bad debt handling。
不要做：現貨撮合、錢包出金。
產出：lending ledger、利息模型、測試。
測試：borrow、repay、interest、forced repayment。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：借貸上線完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 31 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
