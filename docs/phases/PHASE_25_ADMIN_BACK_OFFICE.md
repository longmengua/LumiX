# Phase 25 - 管理後台

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
營運交易所一定要有正式後台。

## 目標
建立 admin RBAC、查詢、審核、四眼批准與稽核紀錄。

## 為何需要這一章
營運交易所一定要有正式後台。

## 依賴
- 前置章節：Phase 21～24。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
user / account / order / trade / deposit / withdrawal 查詢、asset adjustment request、four-eyes approval。

## 非目標
新增交易邏輯。

## 必要產出
admin console、審批流程、審計。

## 驗收標準
後台操作都有角色限制與稽核。

## 必要測試
RBAC、查詢、審批、audit log。

## 可能影響的檔案與模組
admin controllers、services、audit。

## 資料模型影響
後台操作與原因碼。

## API 影響
管理 API。

## 安全影響
最小權限與雙人覆核。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
正式管理完成、正式交易完成。

## 下一階段交接
Phase 26 會把風控與 kill switch 接上。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_25_ADMIN_BACK_OFFICE.md
本章目標：只做 Phase 25 - 管理後台。
範圍：user / account / order / trade / deposit / withdrawal 查詢、asset adjustment request、four-eyes approval。
不要做：新增交易邏輯。
產出：admin console、審批流程、審計。
測試：RBAC、查詢、審批、audit log。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：正式管理完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 25 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
