# P21-T04 唯讀 Order Book Snapshot / Delta Projection 實作審核紀錄

## 任務狀態

```text
Task: P21-T04 Read-Only Order Book Snapshot / Delta Projection
Status: IMPLEMENTATION_REVIEW_APPROVED
Human implementation review: approved
Scope: immutable read-only book projection 與 pure snapshot/delta reducer
Market Data pipeline runtime: not started
External provider / public API / WebSocket / persistence: not started
P21-T05 implementation: approved and completed; P21-T06 to P21-T08: awaiting explicit per-card human approval
HUMAN_REVIEW_REQUIRED: yes
Production claim: prohibited
```

## 完成內容與責任邊界

| 區域 | 已完成內容 | 明確不包含 |
| --- | --- | --- |
| `com.lumix.marketdata.book` | immutable projection/result/status、無狀態 reducer、固定 decision/reason code；公開 projection 建構時驗證 baseline、嚴格排序、正 quantity、crossed health 與上限 | provider adapter、shared cursor map、database、cache、broker、transport |
| book contract 資源邊界 | snapshot 與 delta 每側固定最多 1,024 levels；超限在 payload 建構時以 `BOOK_LEVEL_LIMIT_EXCEEDED` 拒絕 | runtime config、provider 特例、I/O 或部分套用 |
| snapshot | 以 T03 accepted book snapshot 建立 baseline、同價位數量聚合、bid 高至低與 ask 低至高排序、as-of source sequence/time | 外部 snapshot fetch、checksum、正式 order-book service |
| delta | 僅在已有健康或 stale baseline、event sequence 恰為 projection `asOfSequence + 1`，且 T03 accepted cursor identity/sequence 對應目前 event 時套用；零 quantity 移除價位 | matching book、交易 order、fill producer、任何 mutation |
| fail-closed | 初始 delta、out-of-order、gap/resync、跨 baseline admission、duplicate admission mismatch、crossed book、precision overflow、錯誤 stream、payload 超限與套用後 projection 超限均不改寫 levels/as-of metadata/identity 並降級 status | 自動補號、隱式 rounding、wall-clock health 猜測 |
| tests | 固定 fixture 驗證 snapshot/delta、排序、empty book、duplicate/stale、out-of-order、gap/resync snapshot、crossed、overflow、多 instrument、admission mismatch、model 建構防線、level 邊界與 deterministic replay | 外部 provider、secret、真實網路、公開 depth API |

## Book status 規則

| 情況 | 投影狀態 | 是否可作為健康 book |
| --- | --- | --- |
| 已接受且未 stale 的 snapshot/delta | `HEALTHY` | 是，僅限內部唯讀 projection |
| 已接受但 received time stale，或 duplicate 使既有 book stale | `STALE` | 否 |
| T03 偵測 gap | `GAP_DETECTED` | 否 |
| 初始 delta 或 resync 等待中 | `RESYNC_REQUIRED` | 否 |
| crossed book、完整性衝突或 overflow | `DEGRADED` | 否 |
| 尚無 baseline | `UNAVAILABLE` | 否 |

空 snapshot 是明確的已接受資料內容，而非 `UNAVAILABLE`；呼叫端必須同時檢查 `isEmpty()` 與 status，不能把空列表誤認成健康或失效的唯一證據。

## 驗證證據

```text
cd server && ./mvnw -Dtest=NormalizedMarketDataEventContractTest,MarketDataPrecisionContractTest,MarketDataPayloadImmutabilityTest,NormalizedMarketDataEventWireMapperTest,MarketDataStreamAdmissionPolicyTest,ReadOnlyOrderBookProjectionReducerTest test
Result: 36 tests, 0 failures, 0 errors, 0 skipped

./server/mvnw test
Result: 336 tests, 3 failures, 0 errors, 2 skipped
```

本次 baseline/admission 相容性修正新增兩項固定 reason code：`DELTA_SEQUENCE_NOT_CONTINUOUS` 與
`ADMISSION_CURSOR_BASELINE_MISMATCH`。對 delta，reducer 同時驗證 projection baseline、下一個 sequence、
T03 accepted cursor 的最後 accepted identity/sequence；不一致時保留舊 levels、as-of metadata 與 identity，
並標記 `RESYNC_REQUIRED`。`DUPLICATE_IGNORED` 亦必須讓 cursor identity 等於 duplicate event identity，否則明確拒絕為 `ADMISSION_EVENT_MISMATCH`。

P21-T05 已在取得人類明確 implementation approval 後另行施工；其程式、測試與審核證據位於 `aggregation/` 與 `p21-t05-implementation-review.md`，不混入本 T04 的責任邊界。

本次補強讓 `ReadOnlyOrderBookProjection` 的公開建構子不能繞過 reducer 偽造不完整 baseline 的
`HEALTHY`／`STALE` projection，也拒絕未排序或重複價位、零 quantity、`HEALTHY` crossed book 與超過每側
1,024 levels 的 projection。snapshot 與 delta payload 在 contract 建構時使用 `BOOK_LEVEL_LIMIT_EXCEEDED`
fail closed；若單筆合法 delta 套用後才超過 projection 上限，reducer 保留原 levels/as-of metadata/identity、
降為 `DEGRADED` 並回傳 `LEVEL_LIMIT_EXCEEDED`，不會部分套用。

完整 regression 的 3 個失敗均為既有 P15/P16/P17 architecture gate（`P15T09Phase15FinalReviewGateTest`、
`P16T10SpotSandboxFinalReviewGateTest`、`P17T05Phase17FinalReviewGateTest`）硬性期待 `AI_PROGRESS.md`
仍含已過時的 `Phase 21: PLANNED_NOT_STARTED`。目前權威狀態必須如實記錄 P21-T01 到 P21-T04 已批准、
P21-T05 等待 implementation review，故不以回填錯誤狀態或修改禁止範圍的既有 architecture test 掩蓋失敗。
此 regression gate 的更新需另行審核；P21-T02/T04 的 contract、policy 與 projection tests 本身全數通過。

`git diff --check` 與禁止依賴／禁止檔案掃描已通過。

## No-claim 與下一步

本 task 沒有建立 market-data pipeline runtime、provider adapter、API、WebSocket、persistence、cache、event broker 或 shared projection store；更沒有改動 matching、trade/fill、position、balance、ledger、reservation、settlement 或 wallet。它不表示正式行情服務、公開 depth、real-time SLA、正式交易或 production-ready。

P21-T04 implementation review 已獲批准。P21-T05（trade、ticker 與 candle aggregation）已依其獨立 task card 與人類明確批准完成，但仍不得把本 projection 接到 provider、transport、matching 或交易核心。
