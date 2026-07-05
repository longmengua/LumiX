# Phase 17 - C++ 撮合核心

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
撮合必須由獨立核心負責，不能靠 Java 假裝完成。

## 目標
建立確定性撮合、order book、價格時間優先、重放與回復。

## 為何需要這一章
撮合必須由獨立核心負責，不能靠 Java 假裝完成。

## 依賴
- 前置章節：Phase 11 的邊界與 Phase 16 的接點。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
limit / market order、cancel、partial fills、benchmark、C++ 測試。

## 非目標
餘額變動、結算、錢包操作。

## 必要產出
C++ core、測試、benchmark。

## 驗收標準
replay 與 snapshot recovery 結果可重現。

## 必要測試
C++ 單元、整合、benchmark。

## 可能影響的檔案與模組
core/ 或 matching-core/。

## 資料模型影響
事件與 sequence metadata。

## API 影響
撮合邊界協議。

## 安全影響
權威隔離。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
Java 整合完成、結算完成、正式市場資料完成、正式交易完成。

## 下一階段交接
Phase 18 會把 Java 與 C++ 串起來。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_17_CPP_MATCHING_CORE.md
本章目標：只做 Phase 17 - C++ 撮合核心。
範圍：limit / market order、cancel、partial fills、benchmark、C++ 測試。
不要做：餘額變動、結算、錢包操作。
產出：C++ core、測試、benchmark。
測試：C++ 單元、整合、benchmark。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：Java 整合完成、結算完成、正式市場資料完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 17 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
