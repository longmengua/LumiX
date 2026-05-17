# application/scheduler

排程任務層。

目前狀態：
- Funding settlement、snapshot、reconciliation、outbox relay、prediction market sync 都有 scheduler baseline。
- 部分 scheduler 在 MVP 階段可能保留註解掉的 `@Scheduled`，避免本機啟動後自動改動狀態。

注意：
- production 啟用前要確認 idempotency、分散式鎖、重試策略與監控告警。
- 長時間 worker 未來應從 REST app 拆出。
