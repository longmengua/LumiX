# application/scheduler

排程任務層。

目前狀態：
- Funding settlement、snapshot、reconciliation、outbox relay、prediction market sync、bonus credit expiry/clawback、market-maker hedge execution、matching worker lease renewal、market-data retention 都有 scheduler baseline。
- 部分 scheduler 在 MVP 階段可能保留註解掉的 `@Scheduled`，避免本機啟動後自動改動狀態。
- `BonusCreditExpiryScheduler` 預設 `bonus-credit.expiry-enabled=false`，啟用前要確認 ledger/retry/告警策略。
- `BonusCreditClawbackScheduler` 預設 `bonus-credit.clawback-policy.enabled=false`，啟用前要確認 campaign/asset/max-amount 設定、營運核准、worker lock 與告警策略。
- `FinanceExportScheduler` 預設 `finance.export.enabled=false`，啟用後會依 cron 產生前一 UTC 日 fee/funding/liquidation/bonus/transfer category export batch；啟用前要確認日報平衡、archive manifest restore smoke 與告警流程。
- `MarketMakerHedgeExecutionScheduler` 預設 `market-maker.hedge-execution.enabled=false`，production 可開 `market-maker.hedge-execution.lock-enabled=true` 使用 durable worker lock，也可開 `approval-required=true` 要求 operator approval token；啟用前要確認 venue adapter、global halt、告警與對帳策略。
- `MatchingWorkerLeaseRenewalScheduler` 預設 `matching-worker.enabled=false`，啟用前要確認 symbol routing、readiness、舊 REST path halt/fencing 與告警策略。
- `MarketDataRetentionScheduler` 預設 `market-data.retention.enabled=false`，啟用前要確認 archive/export、DB partition 與監控策略。
- `TurnoverReconciliationScheduler` 預設 `turnover.reconciliation.enabled=false`，啟用前要確認 trade tape / ledger journal 延遲、batch size、worker lock 與告警策略。

注意：
- production 啟用前要確認 idempotency、分散式鎖、重試策略與監控告警。
- 長時間 worker 未來應從 REST app 拆出。
