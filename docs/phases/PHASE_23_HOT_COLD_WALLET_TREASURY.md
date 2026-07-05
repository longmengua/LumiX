# Phase 23 - 熱錢包 / 冷錢包金庫

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
交易所需要穩定的資產保管與調度能力。

## 目標
建立熱錢包、冷錢包、補水策略、簽章邊界與對帳。

## 為何需要這一章
交易所需要穩定的資產保管與調度能力。

## 依賴
- 前置章節：Phase 21～22。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
hot wallet、cold wallet、sweep、threshold、batching、signer boundary、HSM / MPC placeholder、alerting。

## 非目標
撮合、帳本、期貨保證金。

## 必要產出
treasury 流程、閾值規則、警報。

## 驗收標準
熱錢包餘額與出金量可以被管理。

## 必要測試
sweep、threshold、reconciliation、alert。

## 可能影響的檔案與模組
treasury、wallet boundary、monitoring。

## 資料模型影響
金庫狀態與轉帳記錄。

## API 影響
內部金庫介面。

## 安全影響
簽章與保管邊界清楚。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
正式錢包安全完成、正式交易完成。

## 下一階段交接
Phase 24 會把對外 API 也納進來。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_23_HOT_COLD_WALLET_TREASURY.md
本章目標：只做 Phase 23 - 熱錢包 / 冷錢包金庫。
範圍：hot wallet、cold wallet、sweep、threshold、batching、signer boundary、HSM / MPC placeholder、alerting。
不要做：撮合、帳本、期貨保證金。
產出：treasury 流程、閾值規則、警報。
測試：sweep、threshold、reconciliation、alert。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：正式錢包安全完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 23 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
