<!-- 檔案用途：定義 Polymarket local、CLOB、trade、settlement order lifecycle transition matrix。 -->
# Polymarket Order Transition Matrix

這份 matrix 是 Polymarket local order state 的營運與實作契約。它記錄目前程式已 guard 的狀態，以及仍列在 production TODO 的轉換。

## State Columns

| Column | Owner | Purpose |
| --- | --- | --- |
| `status` | Local order 與 CLOB lifecycle | `PredictionPolymarketOrder.status` 保存的主要 order lifecycle。 |
| `tradeStatus` | User-channel trade events | `PredictionPolymarketOrder.tradeStatus` 保存的最新 trade lifecycle signal。 |
| `lastTradeId` | User-channel trade events | 最新 observed trade event 的 idempotency 與 operator lookup anchor。 |
| `lastClobPayload` | CLOB sync/cancel 或 user event | 保存最新 remote payload，供 audit 與 replay diagnosis。 |

## Local And CLOB Status Matrix

| Current local status | Incoming source | Incoming status | Allowed local result | Guard / rule |
| --- | --- | --- | --- | --- |
| none / new row | Local place command | `CREATED` | `CREATED` | effectful CLOB submit 前先建立 local idempotency row。 |
| `CREATED` | CLOB place success | accepted/live/matched remote status | remote status as returned | 保存 `clobOrderId`、CLOB payload 與 terminal/active result。 |
| `CREATED` | CLOB place exception 或 uncertain duplicate | `CLOB_OUTCOME_UNCERTAIN` | `CLOB_OUTCOME_UNCERTAIN` | 不盲目重送；後續 reconcile 必須解析 outcome。 |
| active status，例如 `ACCEPTED`、`LIVE`、`ORDER_STATUS_LIVE`、`MATCHED`、`ORDER_STATUS_MATCHED` | CLOB cancel success | `CANCEL_REQUESTED` 或 remote canceled terminal | cancel requested 或 canceled terminal | Cancel command idempotency records 防止 duplicate DELETE effect。 |
| active status | CLOB cancel timeout / 5xx | `CANCEL_OUTCOME_UNCERTAIN` | `CANCEL_OUTCOME_UNCERTAIN` | Reconcile 會納入 uncertain cancel rows，並用 CLOB read-only status 解析。 |
| active 或 uncertain status | CLOB sync/reconcile | terminal remote status，例如 `ORDER_STATUS_CANCELED`、`ORDER_STATUS_FILLED`、`ORDER_STATUS_SETTLED`、`FAILED`、`REJECTED` | terminal remote status | terminal remote progress 可套用。 |
| terminal local status，例如 canceled / filled / settled / failed / rejected | CLOB sync/reconcile | active remote status，例如 `LIVE`、`MATCHED`、`ORDER_STATUS_LIVE`、`ORDER_STATUS_MATCHED` | 保留 terminal local status | `PolymarketOrderStateMachine` 防止 stale active payload 把 local terminal state 降級。 |
| terminal local status | CLOB sync/reconcile | active remote matched size | 保留 local matched size | `shouldApplyRemoteMatchedSize(...)` 會拒絕 stale active matched-size regression。 |
| any status | CLOB sync/reconcile | unchanged payload | 不保存 local row | Sync/reconcile 仍可讀 CLOB，但 unchanged payload 是 no-op local replay。 |

## Trade Event Matrix

| Current local/trade state | Incoming source | Incoming event | Allowed result | Guard / rule |
| --- | --- | --- | --- | --- |
| 有 matching `clobOrderId` 的任意 order | Polymarket user channel | `eventType=trade` 且 status present | 更新 `tradeStatus`、設定 `lastTradeId`、保存 payload | `PolymarketUserEventService` 將 trade events 套到 `tradeStatus`，不直接覆寫 primary `status`。 |
| any order | duplicate user-channel event | 相同 computed `eventKey` | no-op | persisted user event key 讓 replay idempotent；unique-key save race 也視為 duplicate replay。 |
| 找不到 CLOB id 對應 local order | user-channel event | any event | 只保存 event | 直到 matching local order 存在前，不產生 local order side effect。 |

## Settlement Matrix

| Current state | Incoming source | Incoming settlement signal | Allowed result | Guard / rule |
| --- | --- | --- | --- | --- |
| active / matched / filled local order | CLOB sync/reconcile | `SETTLED` 或 `ORDER_STATUS_SETTLED` | primary `status` 轉為 settled | Terminal settlement 是 forward progress，後續 active CLOB payload 不得降級。 |
| settled local order | stale CLOB sync/reconcile | active remote status 或較低 matched size | 保留 settled status 與 matched size | 既有 terminal downgrade guard 適用。 |
| trade-only update | user-channel event | settlement-like payload | TODO | 除非 event 會映射到 primary `status`，否則 settlement-specific user-event persistence / replay tests 仍是 production TODO。 |

## Invariants

- 不因 local row 已存在但沒有 terminal remote outcome 就盲目重送 effectful CLOB place/cancel command。
- Terminal local states 不能被 stale active CLOB reads 降級。
- local status 已是 terminal 且 remote payload 是 active 時，matched size 不能倒退。
- Trade events 必須 replay-idempotent，且更新 `tradeStatus` / `lastTradeId`。
- Settlement 是 terminal；後續 active payload 只能保存為 raw audit data，不能改變 primary terminal state。

## Remaining TODOs

- 將 Polymarket trade events 持久化進 local order lifecycle projection，而不只保存目前最新 `tradeStatus` / `lastTradeId`。
- 補 settlement transition tests，涵蓋 terminal-state downgrade protection 與 user-channel settlement replay。
