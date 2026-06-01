<!-- 檔案用途：定義單 symbol matching sequencer 的 production 部署與 failover 規則。英文版本位於 ../en/matching-sequencer-runbook.md。 -->
# Matching Sequencer Runbook

這份文件定義 per-symbol matching sequencer 的 production 規則。目前實作仍是 in-process，但 production 部署必須保留同一個不變量：同一個 symbol 同一時間只能有一個 active writer 處理撮合指令。

English version：[../en/matching-sequencer-runbook.md](../en/matching-sequencer-runbook.md)

## 不變量

- 一個 symbol 只能有一個 active matching owner。
- 同一 symbol 的所有 command 必須以單一 total order 處理。
- ownership 必須用 epoch 或 lease token fencing，避免舊 owner 在 failover 後繼續寫入。
- recovery 必須先載入最新 snapshot，再從 checkpoint 之後 replay command/event，完成後才能接 live traffic。

## 分區規則

- order、cancel、amend、cancel-replace command 依 normalized `symbol` 路由。
- 同一 symbol 的所有 command 必須進同一個 sequencer partition。
- 不允許把同一 symbol 切到多個 active worker。
- rebalancing 只能在舊 owner 被 fenced 且停止後，才把 symbol 移到新 worker。

## Ownership Lease

Production worker 應從強一致儲存取得 per-symbol lease。

必要 lease 欄位：
- `symbol`
- `ownerId`
- `epoch`
- `expiresAt`
- `lastCheckpoint`

規則：
- recovery 前先使用 `MatchingSequencerLeaseService.acquire(symbol, ownerId)` 取得 ownership。
- 使用 `MatchingSequencerLeaseService.renew(...)` 延長 ownership，並保存 owner 觀察到的 command/event checkpoint。
- planned handoff 時使用 `MatchingSequencerLeaseService.release(...)` 釋放 ownership。
- live command write 前使用 `MatchingSequencerLeaseService.requireWritable(symbol, ownerId, epoch)` 驗證 owner 仍可寫。
- Worker startup 應使用 `MatchingWorkerLifecycleService.startConfiguredSymbols()`，取得 configured symbol lease、執行 recovery，並暴露 owner/epoch context。
- Worker renewal 應使用 `MatchingWorkerLifecycleService.renewOwnedSymbols()` 延長 lease，並保存觀察到的 command/event offset。
- Worker command intake 應導到 `MatchingWorkerExecutionService`，由它先 append lease-fenced command，再套用到 matching engine。
- 每次 command write 都必須帶目前 `epoch`。
- storage 必須拒絕 stale epoch 的寫入。
- backing store 不可用時，worker 必須停止續租，並且不能再接受新 command。
- lease TTL 應長於正常 command processing interval，且短於營運 failover target。

## Worker 設定

Production worker ownership 由 `matching-worker` properties 設定：

| Property | 環境變數 | 意義 |
| --- | --- | --- |
| `matching-worker.enabled` | `MATCHING_WORKER_ENABLED` | 是否啟用獨立 worker command intake。預設 `false`。 |
| `matching-worker.owner-id` | `MATCHING_WORKER_OWNER_ID` | 唯一 worker owner id，通常使用 pod / instance / process identity。 |
| `matching-worker.symbols` | `MATCHING_WORKER_SYMBOLS` | 此 worker 應持有的 symbols，逗號分隔，例如 `BTCUSDT,ETHUSDT`。 |
| `matching-worker.lease-ttl-ms` | `MATCHING_WORKER_LEASE_TTL_MS` | Lease TTL。預設 `30000`。 |
| `matching-worker.renew-interval-ms` | `MATCHING_WORKER_RENEW_INTERVAL_MS` | Lease renew interval。預設 `10000`。 |
| `matching-worker.fence-legacy-routing` | `MATCHING_WORKER_FENCE_LEGACY_ROUTING` | configured symbol 尚未 worker-ready 時，拒絕 fallback 到舊 in-process path。預設 `false`。 |

在 command routing 指向 `MatchingWorkerExecutionService`，且舊 REST / in-process path 對同一批 symbol 已被 halt 或 fencing 前，不要啟用 worker command intake。

Submit、cancel、amend 在 symbol 有 ready owner context 時已會走 worker execution path。Cancel-replace 保留 accounting-safe cancel + replacement-submit flow；ready worker context 下兩段都會是 fenced worker command。

Readiness inspection：
- `GET /api/recovery/matching-worker/contexts`
- `GET /api/recovery/matching-worker/contexts/{symbol}`

## 啟動流程

1. 呼叫 `MatchingWorkerLifecycleService.startConfiguredSymbols()` 或 `startSymbol(symbol)`。
2. 取得 symbol lease，拿到新的 `epoch`。
3. 對該 symbol 呼叫 `MatchingRecoveryService.recoverSymbol(symbol)`。
4. recovery service 會載入最新 matching snapshot、replay checkpoint 之後的 command log、執行 replay validation，並保存恢復後 snapshot 與 validation report。
5. recovery 回傳 valid report 後，才發布該 symbol readiness。
6. 開始接 live command。

當 `matching-worker.enabled=true` 時，`MatchingWorkerStartupListener` 會在 Spring application ready 後呼叫 `startConfiguredSymbols()`。

## Planned Failover

1. 停止把新 command 路由到舊 owner。
2. 等待 in-flight command drain。
3. 持久化最後 snapshot 與 checkpoint。
4. 釋放 lease，或讓新 owner 取得更高 `epoch`。
5. 新 owner 依啟動 recovery flow 接手。

## Deployment Switch Sequence

1. 先以 `MATCHING_WORKER_ENABLED=false` 部署，確認 legacy routing smoke tests 仍通過。
2. 為小範圍 symbol 設定 `MATCHING_WORKER_OWNER_ID` 與 `MATCHING_WORKER_SYMBOLS`。
3. 啟用 `MATCHING_WORKER_ENABLED=true`，但先維持 `MATCHING_WORKER_FENCE_LEGACY_ROUTING=false`。
4. 查 `GET /api/recovery/matching-worker/contexts/{symbol}`，確認 owner id、epoch、lease expiry 都存在。
5. 對該 symbol 執行 submit、cancel、amend、cancel-replace 測試單，確認 matching command log 都帶 worker owner/epoch。
6. 對同一批 symbol 啟用 `MATCHING_WORKER_FENCE_LEGACY_ROUTING=true`，讓 missing readiness 直接拒絕，而不是 fallback 到舊 in-process writer。
7. 監控 lease renewal、command/event offset 推進、replay validation report、order/accounting reconciliation。
8. Rollback 時先設 `MATCHING_WORKER_FENCE_LEGACY_ROUTING=false`，再設 `MATCHING_WORKER_ENABLED=false`；確認沒有 active worker 持有 symbol 後，才把流量導回舊路徑。

## Unplanned Failover

1. 偵測 heartbeat miss 或 lease expire。
2. 推進 lease `epoch`，阻止舊 owner 繼續寫入。
3. 啟動 replacement worker。
4. 從最新 snapshot restore，並 replay checkpoint 之後的 log。
5. recovery readiness 可見後，才恢復 command routing。
6. 對 failover window 內 client accepted command 做 command log audit。

## 營運控制

- symbol 沒有 active owner 時告警。
- 多個 owner 宣稱同一 symbol 時告警。
- checkpoint lag 持續增加時告警。
- replay 失敗或 snapshot age 超過 recovery target 時告警。
- 暴露 per-symbol owner、epoch、checkpoint、replay lag、halted/running status。

## 目前狀態

目前 matching core 已有 durable command/event log、offset checkpoint、snapshot、validation report、recovery orchestration、lease lifecycle、service-level write guard、owner epoch audit 欄位、worker startup / renewal readiness lifecycle、runtime startup hook、readiness inspection endpoints、submit、cancel、amend、cancel-replace accounting-safe cancel + replacement-submit orchestration 的 worker execution / routing，以及明確 legacy-routing fence。

2026-05-29 已完成以下 smoke verification：

```bash
./mvnw -Dtest=MatchingWorkerCommandRouterTest,MatchingWorkerExecutionServiceTest,MatchingWorkerLifecycleServiceTest,OrderAccountingIntegrationTest test
```

剩餘 production hardening 屬於目前 in-process worker baseline 之外的多進程營運強化，不是 worker-routing acceptance criteria 本身。
