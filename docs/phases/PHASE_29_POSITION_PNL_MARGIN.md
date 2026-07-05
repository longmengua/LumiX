# Phase 29 - 倉位 / PnL / 保證金引擎

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
期貨不能只靠現貨帳本，需要倉位與保證金模型。

## 目標
建立開倉、平倉、已實現 / 未實現 PnL、初始保證金與維持保證金。

## 為何需要這一章
期貨不能只靠現貨帳本，需要倉位與保證金模型。

## 依賴
- 前置章節：Phase 13、14、19、28。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
isolated / cross margin、leverage 調整、funding payment settlement。

## 非目標
強平、ADL、保險基金。

## 必要產出
position engine、margin 計算、測試。

## 驗收標準
倉位、PnL 與保證金可被正確計算。

## 必要測試
開平倉、PnL、保證金、funding。

## 可能影響的檔案與模組
position、PnL、margin 服務。

## 資料模型影響
倉位與保證金記錄。

## API 影響
倉位與保證金查詢。

## 安全影響
避免錯算風險。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
期貨交易完成、正式交易完成。

## 下一階段交接
Phase 30 會處理強平與 ADL。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_29_POSITION_PNL_MARGIN.md
本章目標：只做 Phase 29 - 倉位 / PnL / 保證金引擎。
範圍：isolated / cross margin、leverage 調整、funding payment settlement。
不要做：強平、ADL、保險基金。
產出：position engine、margin 計算、測試。
測試：開平倉、PnL、保證金、funding。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：期貨交易完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 29 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
