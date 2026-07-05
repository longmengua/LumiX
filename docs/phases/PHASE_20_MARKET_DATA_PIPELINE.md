# Phase 20 - 市場資料管線

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
市場資料要從撮合事件長出來，不能手工拼。

## 目標
把 order book snapshot、delta、trade tape、ticker、kline 發給前端與 API。

## 為何需要這一章
市場資料要從撮合事件長出來，不能手工拼。

## 依賴
- 前置章節：Phase 17～19。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
snapshot、delta、trade tape、ticker、kline、Redis cache、WebSocket fanout、REST market API、sequence gap 處理。

## 非目標
撮合、帳本、結算。

## 必要產出
market data service、快取、推送、測試。

## 驗收標準
市場資料可以重建，序號缺口會被發現。

## 必要測試
snapshot / delta、重放、序號缺口、回復。

## 可能影響的檔案與模組
market data 服務、WebSocket、REST。

## 資料模型影響
市場資料快取與序號。

## API 影響
公開市場查詢與推播。

## 安全影響
只暴露必要資料。

## 用戶資金影響
- 否。
- 審核需求：必須人工審核。

## 風險等級
High。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
正式交易完成。

## 下一階段交接
Phase 21 開始處理真實入金。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_20_MARKET_DATA_PIPELINE.md
本章目標：只做 Phase 20 - 市場資料管線。
範圍：snapshot、delta、trade tape、ticker、kline、Redis cache、WebSocket fanout、REST market API、sequence gap 處理。
不要做：撮合、帳本、結算。
產出：market data service、快取、推送、測試。
測試：snapshot / delta、重放、序號缺口、回復。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：正式交易完成。
輸出格式：Changed Files、Summary、What Phase 20 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
