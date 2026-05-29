<!-- File purpose: External API retry/idempotency inventory. Chinese version: ../zh-TW/external-api-idempotency.md. -->
# External API Idempotency

## Inventory

| Integration | Current client | Operation type | Retry/circuit/rate limit | Idempotency status |
| --- | --- | --- | --- | --- |
| Gamma market API | `PredictionGammaMarketClient` | Read-only market discovery | Shared OkHttp timeout/retry/circuit/rate-limit baseline | Safe to retry as read-only; response schema versioning still TODO. |
| Polymarket CLOB place/cancel/sync/reconcile | `PolymarketClobTradingClient` via `PolymarketOrderService` / `PolymarketOrderTrackingService` | External effectful writes plus read-only status sync | Shared OkHttp baseline where HTTP client is used | Place baseline: optional `clientRequestId` becomes the local idempotency key. Cancel baseline: recorded cancel statuses replay locally without another DELETE. Sync/reconcile baseline: unchanged CLOB payloads are no-op local writes. |
| Polymarket relayer/RPC approval | `PolymarketSessionService` / `PolymarketApprovalService` / Web3j config | External effectful writes and reads | Shared config partly applies; RPC-specific limits still TODO | Approval reads have a TTL cache and owner-scoped clear. Still TODO: idempotent approval transaction tracking for any backend-observed effectful flow. |
| Hedge venue submit | `HedgeVenueAdapter` | External effectful write | `RetryingHedgeVenueAdapter`, `ThrottlingHedgeVenueAdapter` | Baseline: `IdempotentHedgeVenueAdapter` requires `refId`, claims before venue submit, stores terminal results durably, rejects refId conflicts, and blocks duplicate submits after pending/uncertain outcomes. |
| Bank/chain callbacks | Future callback clients/controllers | External effectful reconciliation | Not implemented | Still TODO. |

## Hedge Submit Baseline

Hedge venue submit uses `HedgeOrderRequest.refId` as the external idempotency key. The idempotency decorator stores a fingerprint of the first payload for each `refId` through `HedgeVenueIdempotencyStore`; Spring uses the JPA-backed store.

- Same `refId` and same payload after an accepted or non-retryable result returns the stored result without another venue call.
- Same `refId` with different payload is rejected with `HEDGE_VENUE_IDEMPOTENCY_CONFLICT`.
- Same `refId` while the first claim is still pending, or after a retryable/timeout-like result, is blocked with `HEDGE_VENUE_OUTCOME_UNCERTAIN`, because the venue may have received the first request.
- Missing `refId` is rejected before any venue call.

The durable baseline still needs venue order lookup/reconciliation, operator handling for uncertain outcomes, and integration-specific rate limits before wiring a real venue adapter.

## CLOB Place Baseline

Polymarket place order accepts optional `clientRequestId`. When present, `PolymarketOrderService` uses it as `internalOrderId` and checks the local order table before consuming session limits, checking approval, signing, or calling CLOB `/order`.

- Same `clientRequestId` and same payload returns the existing local/CLOB result without another CLOB call.
- Same `clientRequestId` with a different user/session/market/direction/amount/type returns `IDEMPOTENCY_CONFLICT`.
- Existing local order without terminal CLOB result returns `CLOB_OUTCOME_UNCERTAIN` and does not call `/order` again.
- Requests without `clientRequestId` keep the previous generated internal id behavior.

## CLOB Cancel Baseline

Polymarket cancel uses the local order status as the retry boundary. If a previous cancel already recorded `CANCEL_REQUESTED`, `CANCELED`, `CANCELLED`, or `ORDER_STATUS_CANCELED`, `PolymarketOrderTrackingService.cancelOrder` returns the local order without another CLOB DELETE.

- First successful CLOB cancel stores the raw CLOB payload, `lastSyncedAt`, and `CANCEL_REQUESTED`.
- Duplicate cancel requests after the local cancel marker do not send another external command.
- This is still a local baseline; fuller CLOB state-machine transitions and remote lookup/reconcile for uncertain cancel outcomes remain TODO.

## CLOB Sync/Reconcile Baseline

CLOB order sync and reconcile are read-only external calls, but they still update the local order row when CLOB state changes. `PolymarketOrderTrackingService` now compares the incoming raw payload, status, matched size, and error before saving.

- Repeated sync of the same CLOB payload returns the local order without another database save.
- Reconcile still counts the order as checked, but reports unchanged rows separately and avoids writing them.
- Changed remote status, matched size, error, or raw payload is saved with a fresh `lastSyncedAt`.
- Durable command identity and fuller local/CLOB/trade/settlement state-machine transitions remain TODO.

## RPC Approval Read Baseline

`PolymarketApprovalService` caches ERC20 allowance and ERC1155 approval reads by contract, owner, and spender/operator for `polymarket.approval-cache-ttl-seconds`.

- Repeated approval reads inside the TTL do not issue another `eth_call`.
- `DELETE /api/prediction/approve/cache?owner=...` clears cache entries for one owner; omitting owner clears all approval caches.
- Expired entries are refreshed from RPC before order validation uses them.
- Backend-observed approval transaction idempotency remains TODO for future effectful relayer flows.
