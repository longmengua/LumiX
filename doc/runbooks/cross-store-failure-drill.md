<!-- 檔案用途：MySQL/Redis/Kafka cross-store failure drill。 -->
# Cross-Store Failure Drill

MySQL 已 commit，但 Redis hot state 或 Kafka/outbox publish 延遲、失敗時使用這份 drill。

## 權威順序

1. MySQL domain state 與 durable outbox rows 在 commit 後是 authoritative。
2. Kafka publish 可從 pending outbox rows 恢復。
3. Redis hot state 是 projection，必須從 MySQL、matching logs 或 lifecycle projections 重建。

## Drill 步驟

1. 在 command commit 後停止 consumer 或阻斷 broker connectivity。
2. 確認 MySQL command state 與對應 outbox row。
3. 執行 outbox/domain-state consistency：
   `GET /api/recovery/outbox/domain-state-consistency?limit=50`
4. 只有 domain state 存在時，才 replay pending/dead outbox：
   `POST /api/recovery/outbox/dead/{outboxId}/replay`
5. Redis hot projections 要從 authoritative source 重建，不要重跑原始 command。
6. 若 outbox row 沒有對應 domain-state transition，先停止該 row 並開 reconciliation issue，再做 manual compensation。

## 通過條件

- database commit 前不發生 external publish。
- rollback 不留下 order、ledger、hedge audit 或 outbox half-state。
- Kafka publish 延遲可從 outbox 恢復。
- Redis 從 authoritative state 修復，而不是靠 command replay。
