# Phase 16 - 正式現貨下單服務

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
先做驗證、預留、持久化，再把單送去撮合。

## 目標
實作驗證、資金計算、預留、持久化、送撮合、取消與查詢。

## 為何需要這一章
先做驗證、預留、持久化，再把單送去撮合。

## 依賴
- 前置章節：Phase 12～15。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
client order id 冪等、訂單生命週期、不可假裝撮合。

## 非目標
本地撮合、結算 runtime、市場資料 runtime。

## 必要產出
order service、持久化、測試。

## 驗收標準
下單流程可持久、可重送、以 reservation 為基礎。

## 必要測試
驗證、冪等、送單 / 取消、查詢。

## 可能影響的檔案與模組
spot service、controllers、repositories、DTO。

## 資料模型影響
orders 表與狀態追蹤。

## API 影響
現貨下單 API。

## 安全影響
請求驗證與冪等。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
撮合完成、結算完成、正式市場資料完成、正式交易完成。

## 下一階段交接
Phase 17 會提供 C++ 撮合核心。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_16_PRODUCTION_SPOT_ORDER.md
本章目標：只做 Phase 16 - 正式現貨下單服務。
範圍：client order id 冪等、訂單生命週期、不可假裝撮合。
不要做：本地撮合、結算 runtime、市場資料 runtime。
產出：order service、持久化、測試。
測試：驗證、冪等、送單 / 取消、查詢。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：撮合完成、結算完成、正式市場資料完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 16 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
