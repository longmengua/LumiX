# Phase 18 - Java 與 C++ 核心整合

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
讓下單服務真的接上撮合核心，而不是只留介面。

## 目標
實作命令與事件協議，讓 Java 與 C++ 核心可以穩定互通。

## 為何需要這一章
讓下單服務真的接上撮合核心，而不是只留介面。

## 依賴
- 前置章節：Phase 16～17。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
submit / cancel、fill event consumer、sequence 保證、duplicate event 處理、replay、backpressure、circuit breaker。

## 非目標
結算 runtime、餘額變動 runtime。

## 必要產出
整合層、事件消費、整合測試。

## 驗收標準
命令與事件能可靠往返，不會重複記帳。

## 必要測試
protocol、重送、replay、backpressure。

## 可能影響的檔案與模組
server 與 core 邊界、integration tests。

## 資料模型影響
事件序號與交付記錄。

## API 影響
Java ↔ C++ 介面。

## 安全影響
避免重複事件與亂序。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
結算完成、正式市場資料完成、正式交易完成。

## 下一階段交接
Phase 19 會根據 fill 事件做結算。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_18_MATCHING_INTEGRATION.md
本章目標：只做 Phase 18 - Java 與 C++ 核心整合。
範圍：submit / cancel、fill event consumer、sequence 保證、duplicate event 處理、replay、backpressure、circuit breaker。
不要做：結算 runtime、餘額變動 runtime。
產出：整合層、事件消費、整合測試。
測試：protocol、重送、replay、backpressure。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：結算完成、正式市場資料完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 18 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
