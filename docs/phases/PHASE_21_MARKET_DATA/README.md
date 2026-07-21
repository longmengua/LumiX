# Phase 21 - 行情資料管線（Market Data Pipeline）

## 目前狀態

```text
TASK_CARDS_DRAFTED_AWAITING_HUMAN_APPROVAL
```

Phase 12–20 已完成對應 review。Phase 21 已收到 task-card planning kickoff；本輪僅完成規劃、架構邊界與審核文件。任何 runtime implementation 仍需經審核的個別 task card 與人類明確實作批准。

## 目標

建立 provider-neutral、唯讀且可決定性重放的行情資料管線 foundation。它只把已正規化的行情事件轉成唯讀 projection、聚合結果與內部查詢/串流契約；不產生交易、不改變資金或交易核心狀態。

## 前置相依

- Phase 12–20 已完成其對應 review；Phase 20 的 sandbox integration gate 是本 phase 的最近依賴。
- 每個 runtime task 必須先取得該 task card 的人類批准，並依下列順序施工。
- 外部 provider adapter、公開 WebSocket 或任何 production endpoint 不屬於目前已提出的 task card；各自需要後續獨立 task card 與批准。

## 範圍

- 行情來源 adapter 的抽象邊界與 provider-neutral normalized event contract。
- 明確 scale 的 decimal 或整數最小單位數值、sequence、時間語意與 schema version。
- duplicate、out-of-order、sequence gap、stale/degraded、snapshot/delta、resync 的處理政策。
- 唯讀 order-book projection，以及 trade、ticker、candle 聚合。
- deterministic replay、recovery、唯讀內部 query/stream contract、backpressure 政策、fixture、指標與審核證據。

## 不在本 Phase 的能力

```text
matching engine execution、order intake mutation、trade 或 fill producer
position / balance / ledger / reservation / settlement mutation
liquidation execution、fee collection、deposit、withdrawal、admin privileged action
public production trading、real-money capability、production launch claim
外部正式行情連線、真實 API key / secret、公開 WebSocket runtime
```

行情事件不得直接修改 `order`、`trade execution state`、`position`、`balance`、`ledger`、`reservation`、`settlement` 或 `wallet`。既有 `trading.core.spot.orderbook` 是 sandbox 交易狀態，亦不得當成行情來源或 Phase 21 projection 的儲存體。

## 核心不變式

1. 價格、數量、成交量一律不得使用 `float` 或 `double`；採整數最小單位或帶明確 scale 的 decimal，並在契約中定義 precision、rounding、overflow 與字串 serialization。
2. 每個 normalized event 至少保存 `source`、`channel`、`instrumentId`、`eventType`、`sequence`、`sourceTimestamp`、`receivedTimestamp`、`schemaVersion` 與 payload；本機時間不是事件 identity。
3. sequence 僅在相同 source/channel/instrument 的流內比較。duplicate 必須冪等；out-of-order 必須被偵測與拒絕/隔離；gap 必須標示並觸發 resync，不能靜默宣稱健康。
4. 區分來源事件時間、系統接收時間與處理完成時間。candle 與 ticker window 一律以來源事件時間分桶；接收時間只供延遲與 stale 偵測。
5. feed health 只可為 `HEALTHY`、`STALE`、`GAP_DETECTED`、`RESYNC_REQUIRED`、`DEGRADED` 或 `STOPPED`；下游必須能辨識非 `HEALTHY`，不得把 stale data 呈現為即時正常行情。
6. 相同初始狀態與相同排序事件序列必須得到相同 order-book、ticker、candle 與 health state；禁止 wall clock、隨機值或非決定性 collection ordering 影響結果。
7. order-book 只有在相容 snapshot 與連續 delta 契約都滿足時才能標示為可用。trade-only feed、ticker feed、UI mock order book 都不是 authoritative order book source；資料不足時必須拒絕建立虛假的 authoritative book。

## 高層 task list

| Task | 名稱 | 目前狀態 |
| --- | --- | --- |
| P21-T01 | 現況盤點、領域邊界與核心不變式 | PROPOSED_AWAITING_HUMAN_REVIEW |
| P21-T02 | Normalized Market Data Event Contract | PROPOSED_AWAITING_HUMAN_REVIEW |
| P21-T03 | Sequence、Duplicate、Gap 與 Feed Health Policy | PROPOSED_AWAITING_HUMAN_REVIEW |
| P21-T04 | 唯讀 Order Book Snapshot / Delta Projection | PROPOSED_AWAITING_HUMAN_REVIEW |
| P21-T05 | Trade、Ticker 與 Candle Aggregation | PROPOSED_AWAITING_HUMAN_REVIEW |
| P21-T06 | Deterministic Replay、Resync 與 Recovery Boundary | PROPOSED_AWAITING_HUMAN_REVIEW |
| P21-T07 | 唯讀內部 Query / Stream Contract 與 Backpressure Policy | PROPOSED_AWAITING_HUMAN_REVIEW |
| P21-T08 | Integration Verification、No-Claim Gate 與 Phase Final Review | PROPOSED_AWAITING_HUMAN_REVIEW |

## Task dependency graph

```text
P21-T01
   |
   v
P21-T02 ---> P21-T03
   |            |\
   |            | +--> P21-T05
   |            +----> P21-T04
   |                    |
   +--------------------+
             |
             v
          P21-T06
             |
             v
          P21-T07
             |
             v
          P21-T08
```

## 測試策略

每張實作卡須以固定 fixture、明確 timestamp 與 deterministic ordering 驗證：正常序列、duplicate、out-of-order、gap、snapshot 後 delta、缺少 snapshot 的 delta、stale、resync、decimal precision、極端數值、空 book、crossed book、多 instrument 隔離、錯誤 instrument、schema version mismatch 與 deterministic replay。除非後續 task 明確批准，測試不得呼叫外部 provider 或使用真實 secret。

## 風險分類與 HUMAN_REVIEW_REQUIRED

本 phase 沒有直接資金 mutation，但行情正確性會成為後續價格顯示、風控與估值的輸入。因此所有 task card 都標示 `HUMAN_REVIEW_REQUIRED: yes`，人類應特別審查數值精度、事件完整性、health 降級、resync、資料權威性與 no-claim 邊界。此標示不表示任何 task 已獲批准。

## Production no-claim boundary

完成本 phase 的 foundation 不代表正式行情服務、交易所行情、公開 API/WebSocket、外部 provider 連線、real-time SLA、正式交易或 production-ready。Phase 21 不授權 matching、ledger、settlement、wallet 或任何 real-money runtime。

## Task-card review 狀態與路由

- 規劃審核：[phase-21-task-card-review.md](phase-21-task-card-review.md)
- proposed first task：[P21-T01 現況盤點、領域邊界與核心不變式](p21-t01-inventory-boundary-invariants.md)
- approval status：awaiting human review；`P21-T01` 尚未取得 implementation approval。
- 全域規劃：[Phase 21–36 規劃計畫](../../planning/PHASE_21_36_PLANNING_PROGRAM.md)
