# Phase 21 - 入金系統

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
真實資產進入交易所的第一步是入金。

## 目標
建立地址生成、鏈上掃描邊界、確認策略、入金偵測與冪等記帳。

## 為何需要這一章
真實資產進入交易所的第一步是入金。

## 依賴
- 前置章節：Phase 12～14。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
address generation、chain scanner / indexer 邊界、confirmation policy、reorg 處理、manual review。

## 非目標
自動出金、撮合、結算。

## 必要產出
deposit service、掃描邊界、測試。

## 驗收標準
可辨識入金、可冪等入帳、可處理重組。

## 必要測試
deposit detect、reorg、idempotent credit。

## 可能影響的檔案與模組
wallet / scanner / deposit domain。

## 資料模型影響
deposits 表與入金狀態。

## API 影響
入金查詢與記錄。

## 安全影響
避免重複入帳與錯帳。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
正式出金完成、正式交易完成。

## 下一階段交接
Phase 22 會在餘額與風控之上做出金。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_21_DEPOSIT_SYSTEM.md
本章目標：只做 Phase 21 - 入金系統。
範圍：address generation、chain scanner / indexer 邊界、confirmation policy、reorg 處理、manual review。
不要做：自動出金、撮合、結算。
產出：deposit service、掃描邊界、測試。
測試：deposit detect、reorg、idempotent credit。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：正式出金完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 21 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
