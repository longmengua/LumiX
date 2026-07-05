# Phase 24 - 正式 Open API

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
對外 API 需要正式安全邊界。

## 目標
建立 API key、權限範圍、簽章、時間戳、IP 白名單與頻率限制。

## 為何需要這一章
對外 API 需要正式安全邊界。

## 依賴
- 前置章節：Phase 16、20。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
order API、account API、market API、withdraw API 限制、API audit log。

## 非目標
新增交易邏輯。

## 必要產出
Open API、權限層、測試。

## 驗收標準
API 會驗證身份、權限與節流。

## 必要測試
簽章、nonce、rate limit、權限。

## 可能影響的檔案與模組
API controllers、auth、audit。

## 資料模型影響
API key 與存取紀錄。

## API 影響
正式對外 API。

## 安全影響
HMAC / RSA、IP 限制、審計。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
High。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
正式 API 完成、正式交易完成。

## 下一階段交接
Phase 25 把管理後台補完整。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_24_PRODUCTION_OPEN_API.md
本章目標：只做 Phase 24 - 正式 Open API。
範圍：order API、account API、market API、withdraw API 限制、API audit log。
不要做：新增交易邏輯。
產出：Open API、權限層、測試。
測試：簽章、nonce、rate limit、權限。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：正式 API 完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 24 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
