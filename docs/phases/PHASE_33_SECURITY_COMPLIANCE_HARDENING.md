# Phase 33 - 安全與合規加固

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
正式營運需要清楚的安全與合規邊界。

## 目標
把威脅模型、秘密管理、API 濫用偵測、KYC / AML 接點與稽核補齊。

## 為何需要這一章
正式營運需要清楚的安全與合規邊界。

## 依賴
- 前置章節：Phase 24～26、32。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
sanctions screening hook、suspicious withdrawal alert、device / session risk、admin anomaly detection、dependency audit。

## 非目標
新增交易核心。

## 必要產出
安全清單、合規接點、修補項。

## 驗收標準
已知高風險點有處理計畫與紀錄。

## 必要測試
威脅檢查、權限檢查、依賴掃描。

## 可能影響的檔案與模組
security docs、認證、稽核與告警。

## 資料模型影響
安全事件與審計。

## API 影響
安全與合規 API 接點。

## 安全影響
這一章本身就是安全主題。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
安全與合規完成、正式交易完成。

## 下一階段交接
Phase 34 會把監控與事故流程補全。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_33_SECURITY_COMPLIANCE_HARDENING.md
本章目標：只做 Phase 33 - 安全與合規加固。
範圍：sanctions screening hook、suspicious withdrawal alert、device / session risk、admin anomaly detection、dependency audit。
不要做：新增交易核心。
產出：安全清單、合規接點、修補項。
測試：威脅檢查、權限檢查、依賴掃描。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：安全與合規完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 33 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
