# Phase 26 - 風控引擎與總開關

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
正式營運一定要能擋錯單、擋異常與暫停風險。

## 目標
建立使用者、交易對與全站的風控限制，以及總開關。

## 為何需要這一章
正式營運一定要能擋錯單、擋異常與暫停風險。

## 依賴
- 前置章節：Phase 16、20、22、25。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
order size、price band、fat finger、withdrawal pause、symbol halt、matching halt、global kill switch。

## 非目標
新增撮合邏輯。

## 必要產出
risk service、停機控制、測試。

## 驗收標準
風控能在下單與出金前阻擋危險請求。

## 必要測試
limit、band、halt、kill switch。

## 可能影響的檔案與模組
risk domain、gateways、admin controls。

## 資料模型影響
風控規則與事件。

## API 影響
風控設定 API。

## 安全影響
可即時停權與停單。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
正式風控完成、正式交易完成。

## 下一階段交接
Phase 27 會補流動性與做市管理。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_26_RISK_ENGINE_KILL_SWITCH.md
本章目標：只做 Phase 26 - 風控引擎與總開關。
範圍：order size、price band、fat finger、withdrawal pause、symbol halt、matching halt、global kill switch。
不要做：新增撮合邏輯。
產出：risk service、停機控制、測試。
測試：limit、band、halt、kill switch。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：正式風控完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 26 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
