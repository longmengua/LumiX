# Phase 27 - 做市與流動性控制

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
交易所要能管理自己的流動性品質。

## 目標
建立做市 API、報價限制、自成交防護與流動性監控。

## 為何需要這一章
交易所要能管理自己的流動性品質。

## 依賴
- 前置章節：Phase 24～26。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
market maker API、quote limits、self-trade prevention、wash trading detection、inventory limit。

## 非目標
新增撮合或清算邏輯。

## 必要產出
liquidity policy、監控、測試。

## 驗收標準
做市與流動性規則可被管理與追蹤。

## 必要測試
quote limit、STP、監控。

## 可能影響的檔案與模組
liquidity policy、risk、admin。

## 資料模型影響
做市設定與監控資料。

## API 影響
做市管理 API。

## 安全影響
避免自成交與洗量。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
High。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
正式做市完成、正式交易完成。

## 下一階段交接
Phase 28 會開始期貨合約底座。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_27_MARKET_MAKER_LIQUIDITY.md
本章目標：只做 Phase 27 - 做市與流動性控制。
範圍：market maker API、quote limits、self-trade prevention、wash trading detection、inventory limit。
不要做：新增撮合或清算邏輯。
產出：liquidity policy、監控、測試。
測試：quote limit、STP、監控。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：正式做市完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 27 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
