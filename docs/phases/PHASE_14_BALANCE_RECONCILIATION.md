# Phase 14 - 餘額投影與帳本對帳

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
使用者看見的餘額要能被重建、比對與追查。

## 目標
從帳本重建可讀餘額，並檢查帳本與餘額是否一致。

## 為何需要這一章
使用者看見的餘額要能被重建、比對與追查。

## 依賴
- 前置章節：Phase 12～13。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
投影重建、差異偵測、卡住 journal 偵測、稽核報告。

## 非目標
自動修復、撮合、結算、錢包 runtime。

## 必要產出
投影工作、對帳報告、測試。

## 驗收標準
帳本與餘額的差異可重建、可追查。

## 必要測試
rebuild、mismatch detection、audit output。

## 可能影響的檔案與模組
ledger read model、reconciliation jobs。

## 資料模型影響
餘額投影表或 view。

## API 影響
唯讀查詢為主。

## 安全影響
避免靜默漂移。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
凍結完成、正式現貨下單完成、撮合完成、結算完成。

## 下一階段交接
Phase 15 會用投影來移動可用與鎖定餘額。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_14_BALANCE_RECONCILIATION.md
本章目標：只做 Phase 14 - 餘額投影與帳本對帳。
範圍：投影重建、差異偵測、卡住 journal 偵測、稽核報告。
不要做：自動修復、撮合、結算、錢包 runtime。
產出：投影工作、對帳報告、測試。
測試：rebuild、mismatch detection、audit output。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：凍結完成、正式現貨下單完成、撮合完成、結算完成。
輸出格式：Changed Files、Summary、What Phase 14 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
