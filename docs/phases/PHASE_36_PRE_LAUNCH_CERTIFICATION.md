# Phase 36 - 上線前驗證與商業就緒

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
就算功能做完，正式上線前還要有營運與商業條件。

## 目標
完成費率、法務、客服、做市合作、展示演練與 go / no-go 決策。

## 為何需要這一章
就算功能做完，正式上線前還要有營運與商業條件。

## 依賴
- 前置章節：Phase 12～35。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
fee schedule、revenue report、listing policy、customer support、legal terms、privacy policy、risk disclosure、withdrawal SLA、market maker agreement、bug bounty、launch rehearsal。

## 非目標
新增 runtime 功能。

## 必要產出
上線審查包、營運文件、演練紀錄。

## 驗收標準
所有前置章節都完成且可審核。

## 必要測試
launch rehearsal、營運清單、客服演練、go / no-go 佐證。

## 可能影響的檔案與模組
營運、法務、客服、發版文件。

## 資料模型影響
上線證據與決策紀錄。

## API 影響
無新增主要 API。

## 安全影響
確認沒有未解重大安全問題。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
正式上線就緒。

## 下一階段交接
沒有下一個實作章節；只剩正式上線決策。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_36_PRE_LAUNCH_CERTIFICATION.md
本章目標：只做 Phase 36 - 上線前驗證與商業就緒。
範圍：fee schedule、revenue report、listing policy、customer support、legal terms、privacy policy、risk disclosure、withdrawal SLA、market maker agreement、bug bounty、launch rehearsal。
不要做：新增 runtime 功能。
產出：上線審查包、營運文件、演練紀錄。
測試：launch rehearsal、營運清單、客服演練、go / no-go 佐證。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：正式上線就緒。
輸出格式：Changed Files、Summary、What Phase 36 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
