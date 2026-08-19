# P21-T03 Sequence、Duplicate、Gap 與 Feed Health Policy 實作審核紀錄

## 任務狀態

```text
Task: P21-T03 Sequence、Duplicate、Gap 與 Feed Health Policy
Status: COMPLETED_AWAITING_IMPLEMENTATION_REVIEW
Human implementation approval: received
Scope: provider-neutral、唯讀、immutable 的 per-stream admission decision 與 feed-health state machine
Market Data pipeline runtime: not started
External provider / public API / WebSocket / projection: not started
P21-T04 to P21-T08: awaiting explicit per-card human approval
HUMAN_REVIEW_REQUIRED: yes
Production claim: prohibited
```

## 完成內容與責任邊界

| 區域 | 已完成內容 | 明確不包含 |
| --- | --- | --- |
| `com.lumix.marketdata.policy` | `FeedHealth`、固定 decision/reason code、immutable cursor/result、stale policy 與 per-stream pure admission state machine | provider adapter、共享 cursor map、timer thread、scheduler、database、cache、broker、transport |
| sequence / identity | 同 stream 的初始 baseline、連續 sequence、duplicate no-op、out-of-order、同 sequence 異 payload integrity conflict 與 stream mismatch 隔離 | 跨 stream 排序、隱式補號、provider reconnect 或 snapshot 下載 |
| gap / resync | gap 當下進入 `GAP_DETECTED`；後續非 snapshot event 進入 `RESYNC_REQUIRED`；book snapshot 才能重建 baseline | 真正的 resync 執行、projection 套用、book 可用性宣告 |
| health | `HEALTHY`、`STALE`、`GAP_DETECTED`、`RESYNC_REQUIRED`、`DEGRADED`、`STOPPED`；stale 僅以注入 evaluation timestamp 與 received timestamp 判定 | wall-clock、source time 當 stale 基準、即時 SLA 或對外 health endpoint |
| tests | 固定 event/timestamp fixture 驗證正常序列、duplicate、初始 delta、conflict、out-of-order、gap、stale、STOPPED、stream 隔離與 deterministic replay | 外部 provider、secret、真實網路連線、projection 或交易核心測試 |

## Health transition 與 resync 規則

| 前一狀態 | 輸入 | decision | 下一狀態 | event 可套用 |
| --- | --- | --- | --- | --- |
| 無 cursor | book snapshot / trade / ticker baseline | `ACCEPTED` | `HEALTHY` 或 `STALE` | 是 |
| 無 cursor | book delta | `RESYNC_REQUIRED` | `RESYNC_REQUIRED` | 否 |
| `HEALTHY` / `STALE` | 相同 sequence、相同 identity | `DUPLICATE_IGNORED` | 原狀或 `STALE` | 否 |
| `HEALTHY` / `STALE` | 較小 sequence | `OUT_OF_ORDER_REJECTED` | 原狀或 `STALE` | 否 |
| `HEALTHY` / `STALE` | 相同 sequence、不同 payload | `INTEGRITY_CONFLICT` | `DEGRADED` | 否 |
| `HEALTHY` / `STALE` | sequence gap | `GAP_DETECTED` | `GAP_DETECTED` | 否 |
| `GAP_DETECTED` / `RESYNC_REQUIRED` / `DEGRADED` | 非 snapshot | `RESYNC_REQUIRED` | `RESYNC_REQUIRED` | 否 |
| `GAP_DETECTED` / `RESYNC_REQUIRED` / `DEGRADED` | book snapshot | `ACCEPTED` | `HEALTHY` 或 `STALE` | 是 |
| `STOPPED` | 任一 event | `STOPPED` | `STOPPED` | 否 |

初始 delta 被拒絕時，cursor 不會偽造最後 accepted 的 sequence 或 identity；它只保存 stream key 與 `RESYNC_REQUIRED`，防止後續程式誤認該 delta 已套用。

## 驗證證據

```text
mvn -f server/pom.xml -Dtest=NormalizedMarketDataEventContractTest,MarketDataPrecisionContractTest,MarketDataPayloadImmutabilityTest,NormalizedMarketDataEventWireMapperTest,MarketDataStreamAdmissionPolicyTest test
Result: 20 tests, 0 failures, 0 errors

mvn -f server/pom.xml test
Result: 320 tests, 3 failures, 0 errors, 2 skipped
```

完整 regression 的 3 個失敗均為既有 P15/P16/P17 architecture gate（`P15T09Phase15FinalReviewGateTest`、
`P16T10SpotSandboxFinalReviewGateTest`、`P17T05Phase17FinalReviewGateTest`）硬性期待 `AI_PROGRESS.md`
仍含已過時的 `Phase 21: PLANNED_NOT_STARTED`。目前權威狀態必須如實記錄 P21-T01、P21-T02 已批准、
P21-T03 等待 implementation review，故不以回填錯誤狀態或修改禁止範圍的既有 architecture test 掩蓋失敗。
此 regression gate 的更新需另行審核；P21-T02/T03 的 contract 與 policy tests 本身全數通過。

`git diff --check` 與禁止依賴／禁止檔案掃描已通過。

## No-claim 與下一步

本 task 沒有建立 market-data pipeline runtime、provider adapter、API、WebSocket、order-book projection、trade/ticker/candle aggregation、persistence、cache、event broker 或 shared cursor store；更沒有改動 matching、trade/fill、position、balance、ledger、reservation、settlement 或 wallet。它不表示正式行情服務、公開行情、real-time SLA、正式交易或 production-ready。

P21-T03 完成後只能等待 implementation review。P21-T04（唯讀 order-book projection）與 P21-T05（trade、ticker 與 candle aggregation）都必須各自取得經審核 task card 與人類明確批准，且不得把 policy 直接接到 provider、transport 或交易核心。
