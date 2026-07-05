# Phase 35 - 生產部署 / CI-CD / 發版

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
正式營運需要穩定交付與回復能力。

## 目標
建立 Docker、部署清單、環境分離、密鑰注入、DB migration pipeline 與回滾策略。

## 為何需要這一章
正式營運需要穩定交付與回復能力。

## 依賴
- 前置章節：Phase 32～34。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
canary、blue-green、backup / restore drill、disaster recovery drill。

## 非目標
新增交易核心。

## 必要產出
部署腳本、CI/CD、回復演練。

## 驗收標準
部署、回滾與備份恢復都有演練。

## 必要測試
build、部署演練、備份恢復。

## 可能影響的檔案與模組
Docker、部署清單、CI/CD、運維文件。

## 資料模型影響
發版與回滾紀錄。

## API 影響
部署與環境設定。

## 安全影響
密鑰與環境隔離。

## 用戶資金影響
- 否。
- 審核需求：必須人工審核。

## 風險等級
High。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
正式上線完成。

## 下一階段交接
Phase 36 會做最終上線審核。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_35_PRODUCTION_INFRA_CICD_RELEASE.md
本章目標：只做 Phase 35 - 生產部署 / CI-CD / 發版。
範圍：canary、blue-green、backup / restore drill、disaster recovery drill。
不要做：新增交易核心。
產出：部署腳本、CI/CD、回復演練。
測試：build、部署演練、備份恢復。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：正式上線完成。
輸出格式：Changed Files、Summary、What Phase 35 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
