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
- 每次 command write 都必須帶目前 `epoch`。
- storage 必須拒絕 stale epoch 的寫入。
- backing store 不可用時，worker 必須停止續租，並且不能再接受新 command。
- lease TTL 應長於正常 command processing interval，且短於營運 failover target。

## 啟動流程

1. 取得 symbol lease，拿到新的 `epoch`。
2. 載入該 symbol 最新 matching snapshot。
3. 讀取 snapshot 內的 checkpoint。
4. replay checkpoint 之後的 command/event log。
5. replay 完成後才發布該 symbol readiness。
6. 開始接 live command。

## Planned Failover

1. 停止把新 command 路由到舊 owner。
2. 等待 in-flight command drain。
3. 持久化最後 snapshot 與 checkpoint。
4. 釋放 lease，或讓新 owner 取得更高 `epoch`。
5. 新 owner 依啟動 recovery flow 接手。

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

## 目前缺口

目前 `InMemoryMatchingEngine` 只有 in-process sequencer 與 snapshot export/restore baseline。它還沒有 durable command log、durable event log、epoch-fenced writes、distributed lease 或 production worker routing。
