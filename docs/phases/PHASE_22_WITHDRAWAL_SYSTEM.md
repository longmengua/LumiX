# Phase 22 - 出金系統

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
出金是最敏感的資金操作之一。

## 目標
建立出金申請、可用餘額檢查、預留、審核、白名單與廣播邊界。

## 為何需要這一章
出金是最敏感的資金操作之一。

## 依賴
- 前置章節：Phase 12～15、21。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
withdrawal request、risk review、address whitelist、fee deduction、tx status tracking、failed release。

## 非目標
自動清算撮合、期貨保證金。

## 必要產出
withdrawal service、審批流程、測試。

## 驗收標準
出金前會預留資金，失敗時可以釋放。

## 必要測試
request、approval、broadcast boundary、failed release。

## 可能影響的檔案與模組
withdrawal domain、admin review、wallet boundary。

## 資料模型影響
withdrawals 表與狀態。

## API 影響
出金申請與查詢。

## 安全影響
白名單、審核、費用檢查。

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
Phase 23 會補上金庫與錢包分層。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_22_WITHDRAWAL_SYSTEM.md
本章目標：只做 Phase 22 - 出金系統。
範圍：withdrawal request、risk review、address whitelist、fee deduction、tx status tracking、failed release。
不要做：自動清算撮合、期貨保證金。
產出：withdrawal service、審批流程、測試。
測試：request、approval、broadcast boundary、failed release。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：正式錢包安全完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 22 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
