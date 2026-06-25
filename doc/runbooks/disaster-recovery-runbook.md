<!-- 檔案用途：matching/order/account/position restore 的 production disaster-recovery runbook。 -->
# Disaster Recovery Runbook

process crash、worker takeover 或 data-store recovery 後，恢復 matching、orders、accounts、positions 時使用。

## 權威順序

1. Matching snapshot 加 command/event logs 定義 order book。
2. Order lifecycle event/projection records 定義訂單最新狀態。
3. Wallet ledger journal 與 account snapshots 定義帳戶餘額。
4. Position repository 與 replayed trade events 定義 open positions。
5. Redis hot state 是 rebuild target，不是 source of truth。

## Matching Worker Takeover

1. 停止受影響 symbol 流量，或啟用 legacy-routing fence。
2. 取得更高 owner epoch 的 sequencer lease。
3. 呼叫 `MatchingRecoveryService.recoverSymbol(symbol)` 或啟動 configured worker。
4. 檢查 command offset、event offset、match sequence 與 validation issues。
5. 重開流量前先跑下方 smoke commands。

## Authenticated Command Reconnect

1. reconnect client 必須維持同一個 authenticated principal，且對 outcome unknown 的指令重用穩定的 `clientOrderId` / command id。
2. 重新送出 submit、cancel、amend 或 cancel-replace 前，先查 order lifecycle projection 與 recovery consistency reports，判斷前一次指令是否已進入 terminal 或 accepted state。
3. 只有在沒有對應 lifecycle projection、command-log entry 或 outbox/domain-state transition 時，才可 replay 該 command。
4. cancel-on-disconnect session 應在舊 close event 被處理前，用 `resumeConnectionId` reconnect，然後先 reconcile 該 uid/symbol 的 open orders，再送新單。
5. session replay 後，跑下方 smoke commands，確認沒有 duplicate client command 或 missing lifecycle projection issue。

## Smoke Commands

```bash
curl -sS "http://localhost:8080/api/recovery/matching-worker/contexts"
curl -sS "http://localhost:8080/api/recovery/restore/account-position-consistency"
curl -sS "http://localhost:8080/api/recovery/outbox/domain-state-consistency?limit=50"
curl -sS "http://localhost:8080/api/recovery/reconcile/accounts"
```

每個 restored symbol 都要送一筆小額 post-only order、取消它，並確認 lifecycle projection 與 outbox consistency 後再開 full traffic。

## 通過條件

- Matching recovery validation valid。
- snapshot + command log 預期存在的 open orders 都在 book 上。
- Account/position consistency report valid。
- Outbox/domain-state consistency 沒有 missing lifecycle projection issues。
- Reconciliation 沒有 account mismatches。
