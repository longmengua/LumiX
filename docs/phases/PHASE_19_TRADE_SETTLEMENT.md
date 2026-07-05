# Phase 19 - 成交結算引擎

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
撮合完成不代表資金完成，還要把成交真的寫進帳本。

## 目標
根據 fill 事件完成手續費、帳本記錄、未用預留釋放與訂單收尾。

## 為何需要這一章
撮合完成不代表資金完成，還要把成交真的寫進帳本。

## 依賴
- 前置章節：Phase 13～18。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
maker / taker fee、reserve commit、unused reserve release、settlement journal、冪等、失敗補償。

## 非目標
市場資料、鏈上錢包 runtime。

## 必要產出
settlement engine、測試、狀態更新。

## 驗收標準
成交後資金、費用與訂單狀態都會落地。

## 必要測試
fill settlement、費用、冪等、失敗補償。

## 可能影響的檔案與模組
settlement domain、ledger 整合、order final state。

## 資料模型影響
settlement journal。

## API 影響
內部結算介面。

## 安全影響
避免重複結算與漏結算。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
正式市場資料完成、正式交易完成。

## 下一階段交接
Phase 20 會把成交結果送去市場資料管線。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_19_TRADE_SETTLEMENT.md
本章目標：只做 Phase 19 - 成交結算引擎。
範圍：maker / taker fee、reserve commit、unused reserve release、settlement journal、冪等、失敗補償。
不要做：市場資料、鏈上錢包 runtime。
產出：settlement engine、測試、狀態更新。
測試：fill settlement、費用、冪等、失敗補償。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：正式市場資料完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 19 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
