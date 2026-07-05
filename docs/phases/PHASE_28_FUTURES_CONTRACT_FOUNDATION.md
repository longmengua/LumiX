# Phase 28 - 期貨合約基礎

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
期貨需要自己的產品規則。

## 目標
建立合約定義、tick、lot、funding interval、index price 與 mark price。

## 為何需要這一章
期貨需要自己的產品規則。

## 依賴
- 前置章節：Phase 20、24、26。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
contract definition、leverage、margin mode、risk limit tier。

## 非目標
倉位引擎、強平、借貸。

## 必要產出
contract models、設定、測試。

## 驗收標準
每個合約規則可被清楚描述與驗證。

## 必要測試
合約設定、價格來源、基礎檢查。

## 可能影響的檔案與模組
futures config、market data、risk。

## 資料模型影響
合約與風險階層。

## API 影響
合約查詢 API。

## 安全影響
避免錯誤合約規格。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
High。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
期貨上線完成、正式交易完成。

## 下一階段交接
Phase 29 會處理倉位、PnL 與保證金。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_28_FUTURES_CONTRACT_FOUNDATION.md
本章目標：只做 Phase 28 - 期貨合約基礎。
範圍：contract definition、leverage、margin mode、risk limit tier。
不要做：倉位引擎、強平、借貸。
產出：contract models、設定、測試。
測試：合約設定、價格來源、基礎檢查。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：期貨上線完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 28 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
