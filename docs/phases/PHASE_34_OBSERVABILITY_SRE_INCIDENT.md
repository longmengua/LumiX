# Phase 34 - 監控 / SRE / 事故應對

## 章節狀態
- 規劃中
- 尚未開始
- 未完成正式上線

## 這一章在交易所中的角色
正式服務不能只做完，還要看得見、救得回。

## 目標
建立結構化 log、metrics、tracing、告警、值班與事故流程。

## 為何需要這一章
正式服務不能只做完，還要看得見、救得回。

## 依賴
- 前置章節：Phase 19～22、32～33。
- 阻塞風險：需求不清、邊界不明、測試不足。

## 範圍
order latency、matching latency、wallet 告警、ledger imbalance 告警、reconciliation 告警、on-call runbook、postmortem template。

## 非目標
新增交易核心。

## 必要產出
監控面板、告警、事故手冊。

## 驗收標準
關鍵狀態都能被觀察與通知。

## 必要測試
告警演練、值班演練、事故模板。

## 可能影響的檔案與模組
observability、runbook、incident docs。

## 資料模型影響
監控與事故紀錄。

## API 影響
監控與告警輸出。

## 安全影響
事故時可快速定位與收斂。

## 用戶資金影響
- 否。
- 審核需求：必須人工審核。

## 風險等級
High。

## 審核門檻
必須人工審核。

## 目前仍不能宣稱
正式上線準備完成。

## 下一階段交接
Phase 35 會整理正式部署與 CI/CD。

## 人工審核要求
這一章完成後，必須先由人工確認。
允許的暫時狀態只有：implementation completed / pending human review。
只有收到明確批准後，才可標記為 completed。

## Codex 實作提示
~~~text
重新讀取 repo，不要沿用舊上下文。
先閱讀：README.md、server/README.md、docs/OPERATING_EXCHANGE_MASTER_PLAN.md、docs/PHASE_REVIEW_WORKFLOW.md、docs/phases/PHASE_34_OBSERVABILITY_SRE_INCIDENT.md
本章目標：只做 Phase 34 - 監控 / SRE / 事故應對。
範圍：order latency、matching latency、wallet 告警、ledger imbalance 告警、reconciliation 告警、on-call runbook、postmortem template。
不要做：新增交易核心。
產出：監控面板、告警、事故手冊。
測試：告警演練、值班演練、事故模板。
更新文件：總綱與本章文件。
驗證命令：cd server && ./mvnw test && ./mvnw package；cd web && npm install && npm run build；若有 test script 再跑 npm test。
不能宣稱：正式上線準備完成。
輸出格式：Changed Files、Summary、What Phase 34 completed、What is still NOT completed、Validation Results、Next Recommended Command。
~~~
