<!-- File purpose: Transition matrix for Polymarket local, CLOB, trade, and settlement order lifecycle states. -->
# Polymarket Order Transition Matrix

This matrix is the operator and implementation contract for local Polymarket order state. It documents the states already guarded by code and the transitions still tracked as production TODOs.

## State Columns

| Column | Owner | Purpose |
| --- | --- | --- |
| `status` | Local order and CLOB lifecycle | Primary order lifecycle stored on `PredictionPolymarketOrder.status`. |
| `tradeStatus` | User-channel trade events | Latest trade lifecycle signal stored on `PredictionPolymarketOrder.tradeStatus`. |
| `lastTradeId` | User-channel trade events | Idempotency and operator lookup anchor for the latest observed trade event. |
| `lastClobPayload` | CLOB sync/cancel or user event | Raw latest remote payload retained for audit and replay diagnosis. |

## Local And CLOB Status Matrix

| Current local status | Incoming source | Incoming status | Allowed local result | Guard / rule |
| --- | --- | --- | --- | --- |
| none / new row | Local place command | `CREATED` | `CREATED` | Local idempotency row is created before effectful CLOB submit. |
| `CREATED` | CLOB place success | accepted/live/matched remote status | remote status as returned | Save `clobOrderId`, CLOB payload, and terminal/active result. |
| `CREATED` | CLOB place exception or uncertain duplicate | `CLOB_OUTCOME_UNCERTAIN` | `CLOB_OUTCOME_UNCERTAIN` | Do not resubmit blindly; later reconcile must resolve outcome. |
| active status such as `ACCEPTED`, `LIVE`, `ORDER_STATUS_LIVE`, `MATCHED`, `ORDER_STATUS_MATCHED` | CLOB cancel success | `CANCEL_REQUESTED` or remote canceled terminal | cancel requested or canceled terminal | Cancel command idempotency records prevent duplicate DELETE effects. |
| active status | CLOB cancel timeout / 5xx | `CANCEL_OUTCOME_UNCERTAIN` | `CANCEL_OUTCOME_UNCERTAIN` | Reconcile includes uncertain cancel rows and resolves from CLOB read-only status. |
| active or uncertain status | CLOB sync/reconcile | terminal remote status such as `ORDER_STATUS_CANCELED`, `ORDER_STATUS_FILLED`, `ORDER_STATUS_SETTLED`, `FAILED`, `REJECTED` | terminal remote status | Terminal remote progress is accepted. |
| terminal local status such as canceled / filled / settled / failed / rejected | CLOB sync/reconcile | active remote status such as `LIVE`, `MATCHED`, `ORDER_STATUS_LIVE`, `ORDER_STATUS_MATCHED` | keep terminal local status | `PolymarketOrderStateMachine` prevents stale active payloads from downgrading terminal local state. |
| terminal local status | CLOB sync/reconcile | active remote matched size | keep local matched size | `shouldApplyRemoteMatchedSize(...)` rejects stale active matched-size regression. |
| any status | CLOB sync/reconcile | unchanged payload | no local save | Sync/reconcile may still read CLOB, but unchanged payload is a no-op local replay. |

## Trade Event Matrix

| Current local/trade state | Incoming source | Incoming event | Allowed result | Guard / rule |
| --- | --- | --- | --- | --- |
| any order with matching `clobOrderId` | Polymarket user channel | `eventType=trade`, status present | update `tradeStatus`, set `lastTradeId`, retain payload | `PolymarketUserEventService` applies trade events to `tradeStatus`, not primary `status`. |
| any order | duplicate user-channel event | same computed `eventKey` | no-op | Persisted user event key makes replay idempotent; unique-key save races are treated as duplicate replay. |
| order not found by CLOB id | user-channel event | any event | event persisted only | No local order side effect until a matching local order exists. |

## Settlement Matrix

| Current state | Incoming source | Incoming settlement signal | Allowed result | Guard / rule |
| --- | --- | --- | --- | --- |
| active / matched / filled local order | CLOB sync/reconcile | `SETTLED` or `ORDER_STATUS_SETTLED` | primary `status` becomes settled | Terminal settlement is forward progress and must not be downgraded by later active CLOB payloads. |
| settled local order | stale CLOB sync/reconcile | active remote status or lower matched size | keep settled status and matched size | Existing terminal downgrade guard applies. |
| trade-only update | user-channel event | settlement-like payload | TODO | Settlement-specific user-event persistence and replay tests remain a production TODO unless the event also maps to primary `status`. |

## Invariants

- Never resubmit an effectful CLOB place/cancel command only because a local row exists without a terminal remote outcome.
- Terminal local states cannot be downgraded by stale active CLOB reads.
- Matched size cannot regress when local status is already terminal and remote payload is active.
- Trade events are replay-idempotent and update `tradeStatus` / `lastTradeId`.
- Settlement is terminal; later active payloads must be retained as raw audit data but must not change primary terminal state.

## Remaining TODOs

- Persist Polymarket trade events into the local order lifecycle projection beyond the current latest `tradeStatus` / `lastTradeId` fields.
- Add settlement transition tests for terminal-state downgrade protection and user-channel settlement replay.
