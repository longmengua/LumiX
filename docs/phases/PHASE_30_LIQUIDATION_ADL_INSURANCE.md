# Phase 30 - 強平 / ADL / 保險基金

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
槓桿交易一定要有失控保護。

## 目標
建立強平觸發、部分強平、破產價、強平單、ADL 隊列與保險基金。

## 為何需要這一章
槓桿交易一定要有失控保護。

## 依賴
- 前置章節：Phase 28～29。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
liquidation trigger、bad debt handling、simulation tests、chaos tests。

## 非目標
現貨撮合、錢包出入金。

## 必要產出
liquidation flow、保險基金、測試。

## 驗收標準
風險過高時可自動處理並留下紀錄。

## 必要測試
強平模擬、混沌測試、審計。

## 可能影響的檔案與模組
liquidation、ADL、insurance fund。

## 資料模型影響
強平與保險基金資料。

## API 影響
風控與處置查詢。

## 安全影響
避免壞帳擴散。

## 用戶資金影響
- 是。
- 審核需求：必須人工審核。

## 風險等級
Critical。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
槓桿安全完成、正式交易完成。

## 下一階段交接
Phase 31 會補借貸模型。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_30_LIQUIDATION_ADL_INSURANCE.md
本章目標：只做 Phase 30 - 強平 / ADL / 保險基金。
範圍：liquidation trigger、bad debt handling、simulation tests、chaos tests。
不要做：現貨撮合、錢包出入金。
產出：liquidation flow、保險基金、測試。
測試：強平模擬、混沌測試、審計。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：槓桿安全完成、正式交易完成。
輸出格式：Changed Files、Summary、What Phase 30 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
