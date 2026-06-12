# application/scheduler

排程任務層。

目前狀態：
- Funding settlement、snapshot、reconciliation、outbox relay、prediction market sync、bonus credit expiry/clawback、market-maker hedge execution、market-maker quote repair、matching worker lease renewal、market-data retention 都有 scheduler baseline。
- 部分 scheduler 在 MVP 階段可能保留註解掉的 `@Scheduled`，避免本機啟動後自動改動狀態。
- `MarketMakerAutoQuoteRunner` 不依賴全域 `@EnableScheduling`，只在 `market-maker.auto-quote.enabled=true`
  時啟動獨立 lifecycle loop；它會透過既有 quote lifecycle 做撤舊單、風控 state 與 WebSocket 推送。
- `BonusCreditExpiryScheduler` 預設 `bonus-credit.expiry-enabled=false`，啟用前要確認 ledger/retry/告警策略。
- `BonusCreditClawbackScheduler` 預設 `bonus-credit.clawback-policy.enabled=false`，啟用前要確認 campaign/asset/max-amount 設定、營運核准、worker lock 與告警策略。
- `FinanceExportScheduler` 預設 `finance.export.enabled=false`，啟用後會依 cron 產生前一 UTC 日 fee/funding/liquidation/bonus/transfer category export batch；啟用前要確認日報平衡、archive manifest restore smoke 與告警流程。
- `ArchiveExporterScheduler` 預設 `archive.exporter.enabled=false`，啟用後會產生 historical orders、trades、ledger archive plan skeleton；啟用前要確認 object storage、manifest review、restore smoke 與 delete guard 流程。
- `MarketMakerQuoteRepairScheduler` 預設 `market-maker.quote-repair.enabled=false`，啟用後會定期撤掉未追蹤的殘留 quote order，並對缺失單邊 tracked leg 的 quote state fail-closed 停用；啟用前要確認 cancel use case、告警與 operator reconciliation 流程。
- `MarketMakerHedgeExecutionScheduler` 預設 `market-maker.hedge-execution.enabled=false`，production 可開 `market-maker.hedge-execution.lock-enabled=true` 使用 durable worker lock，也可開 `approval-required=true` 要求 operator approval token；啟用前要確認 venue adapter、global halt、告警與對帳策略。
- `MatchingWorkerLeaseRenewalScheduler` 預設 `matching-worker.enabled=false`，啟用前要確認 symbol routing、readiness、舊 REST path halt/fencing 與告警策略。
- `MarketDataRetentionScheduler` 預設 `market-data.retention.enabled=false`，啟用前要確認 archive/export、DB partition 與監控策略。
- `PushGatewayHeartbeatScheduler` 預設 `push-gateway.heartbeat.enabled=false`，啟用後會定期向 SSE/WebSocket channel 發送 `gateway.heartbeat`，啟用前要確認 gateway 部署、client timeout 與監控策略。
- `TurnoverReconciliationScheduler` 預設 `turnover.reconciliation.enabled=false`，啟用前要確認 trade tape / ledger journal 延遲、batch size、worker lock 與告警策略。

注意：
- production 啟用前要確認 idempotency、分散式鎖、重試策略與監控告警。
- 長時間 worker 未來應從 REST app 拆出。
