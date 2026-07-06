# Phase 32 - 對帳與補償系統

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
正式營運一定要能找出差異與卡住狀態。

## 目標
把帳本、餘額、訂單、成交、錢包與鏈上資料做跨域對帳。

## 為何需要這一章
正式營運一定要能找出差異與卡住狀態。

## 依賴
- 前置章節：Phase 14、19～22。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
ledger vs balance、order vs trade、matching event vs DB、wallet vs chain、fee revenue、compensation workflow。

## 非目標
自動修帳、不經批准的資產修復。

## 參考文件
- `docs/architecture/data-and-event-flow.md`
- `docs/architecture/order-settlement-flow.md`

## 必要產出
reconciliation jobs、報表、補償流程。

## 驗收標準
差異可被找出，補償有人工流程。

## 必要測試
對帳、卡住狀態、補償流程。

## 可能影響的檔案與模組
reconciliation、報表、運營工具。

## 資料模型影響
對帳案例與補償記錄。

## API 影響
對帳查詢。

## 安全影響
避免靜默修復。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
對帳完成、正式交易完成。

## 下一階段交接
Phase 33 會做安全與合規加固。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_32_RECONCILIATION_COMPENSATION.md
本章目標：只做 Phase 32 - 對帳與補償系統。
範圍：ledger vs balance、order vs trade、matching event vs DB、wallet vs chain、fee revenue、compensation workflow。
不要做：自動修帳、不經批准的資產修復。
產出：reconciliation jobs、報表、補償流程。
測試：對帳、卡住狀態、補償流程。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：對帳完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 32 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
