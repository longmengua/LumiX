<!-- File purpose: External API retry/idempotency inventory. Chinese version: ../zh-TW/external-api-idempotency.md. -->
# External API Idempotency

## Inventory

| Integration | Current client | Operation type | Retry/circuit/rate limit | Idempotency status |
| --- | --- | --- | --- | --- |
| Gamma market API | `PredictionGammaMarketClient` | Read-only market discovery | Shared OkHttp timeout/retry/circuit/rate-limit baseline | Safe to retry as read-only; response schema versioning still TODO. |
| Polymarket CLOB place/cancel/sync/reconcile | `PolymarketClobTradingClient` via `PolymarketOrderService` / `PolymarketOrderTrackingService` | External effectful writes plus read-only status sync | Shared OkHttp baseline where HTTP client is used | Place baseline: optional `clientRequestId` becomes the local idempotency key. Cancel baseline: recorded cancel and uncertain statuses replay locally without another DELETE. Sync/reconcile baseline: unchanged CLOB payloads are no-op local writes. |
| Polymarket relayer/RPC approval | `PolymarketSessionService` / `PolymarketApprovalService` / `RpcTransactionTrackingService` / Web3j config | External effectful writes and reads | Shared config partly applies; RPC-specific limits still TODO | Approval reads have a TTL cache and owner-scoped clear. Backend-observed effectful RPC flows can use durable `commandId` + `txHash` tracking and unresolved outcome reporting. |
| Hedge venue submit/callback | `HedgeVenueAdapter`, `MarketMakerHedgeFillService` | External effectful write plus callback reconciliation | `RetryingHedgeVenueAdapter`, `ThrottlingHedgeVenueAdapter`, `RealHedgeVenueAdapter` HTTP transport | Submit baseline: `IdempotentHedgeVenueAdapter` requires `refId`, claims before venue submit, stores terminal results durably, rejects refId conflicts, and blocks duplicate submits after pending/uncertain outcomes. Callback baseline: `venueOrderId + venueFillId` replays the existing hedge fill audit row. Real venue baseline signs submit/lookup requests, sends them through shared OkHttp, and maps accepted/rejected/retryable venue responses. |
| Bank/chain callbacks | Future callback clients/controllers | External effectful reconciliation | Not implemented | Still TODO. |

## Hedge Submit Baseline

Hedge venue submit uses `HedgeOrderRequest.refId` as the external idempotency key. The idempotency decorator stores a fingerprint of the first payload for each `refId` through `HedgeVenueIdempotencyStore`; Spring uses the JPA-backed store.

- Same `refId` and same payload after an accepted or non-retryable result returns the stored result without another venue call.
- Same `refId` with different payload is rejected with `HEDGE_VENUE_IDEMPOTENCY_CONFLICT`.
- Same `refId` while the first claim is still pending, or after a retryable/timeout-like result, is blocked with `HEDGE_VENUE_OUTCOME_UNCERTAIN`, because the venue may have received the first request.
- Missing `refId` is rejected before any venue call.

Operators can inspect unresolved hedge venue idempotency outcomes with `GET /api/market-maker/hedge-idempotency/unresolved`. The report lists pending claims and completed retryable outcomes without exposing the stored payload fingerprint. `POST /api/market-maker/hedge-idempotency/reconcile` asks the configured `HedgeVenueOrderLookupAdapter` to resolve those outcomes by `refId`; the default adapter is a safe no-op until a real venue lookup is wired.

The durable baseline still needs venue-specific field mapping, production credential rotation, and integration-specific rate limits before wiring a live adapter.

## Hedge Fill Callback Baseline

Venue fill callbacks use `venueOrderId + venueFillId` as the idempotency key. `MarketMakerHedgeFillService.recordVenueFill(...)` checks the durable fill store before append and returns the existing audit row when the same callback is replayed.

- Duplicate venue fill callbacks do not create another hedge fill audit record.
- The JPA store keeps the existing unique constraint on `venue_order_id + venue_fill_id` as a database-level guard.
- Same fill key with conflicting payload still needs venue-specific reconciliation policy before a real adapter is wired.

## CLOB Place Baseline

Polymarket place order accepts optional `clientRequestId`. When present, `PolymarketOrderService` uses it as `internalOrderId` and checks the local order table before consuming session limits, checking approval, signing, or calling CLOB `/order`.

- Same `clientRequestId` and same payload returns the existing local/CLOB result without another CLOB call.
- Same `clientRequestId` with a different user/session/market/direction/amount/type returns `IDEMPOTENCY_CONFLICT`.
- Existing local order without terminal CLOB result returns `CLOB_OUTCOME_UNCERTAIN` and does not call `/order` again.
- Requests without `clientRequestId` keep the previous generated internal id behavior.

## CLOB Cancel Baseline

Polymarket cancel uses an optional `commandId` plus local order status as retry boundaries. If `commandId` is present, `PolymarketOrderTrackingService` claims it in `PolymarketClobCommandStore` before CLOB DELETE and completes the record after the local result is saved. If a previous cancel already recorded `CANCEL_REQUESTED`, `CANCEL_OUTCOME_UNCERTAIN`, `CANCELED`, `CANCELLED`, or `ORDER_STATUS_CANCELED`, it returns the local order without another CLOB DELETE.

- Same `commandId` and same cancel payload returns the local order without another CLOB call.
- Same `commandId` with a different internal/CLOB order is rejected before any CLOB call.
- First successful CLOB cancel stores the raw CLOB payload, `lastSyncedAt`, and `CANCEL_REQUESTED`.
- CLOB cancel `EXCEPTION` or 5xx outcome stores the raw payload, `lastSyncedAt`, `lastError`, and `CANCEL_OUTCOME_UNCERTAIN`.
- Duplicate cancel requests after the local cancel marker do not send another external command.
- Reconcile includes `CANCEL_OUTCOME_UNCERTAIN` orders and can replace the uncertain local state with the remote CLOB status.
- This is still a local baseline; fuller CLOB state-machine transitions remain TODO.

## CLOB Sync/Reconcile Baseline

CLOB order sync and reconcile are read-only external calls, but they still update the local order row when CLOB state changes. `PolymarketOrderTrackingService` now compares the incoming raw payload, status, matched size, and error before saving.

- Repeated sync of the same CLOB payload returns the local order without another database save.
- Reconcile still counts the order as checked, but reports unchanged rows separately and avoids writing them.
- Changed remote status, matched size, error, or raw payload is saved with a fresh `lastSyncedAt`.
- Stale active CLOB payloads cannot downgrade local filled/settled terminal orders or matched size.
- Fuller local/CLOB/trade/settlement event payload transitions remain TODO.

## RPC Approval Read Baseline

`PolymarketApprovalService` caches ERC20 allowance and ERC1155 approval reads by contract, owner, and spender/operator for `polymarket.approval-cache-ttl-seconds`.

- Repeated approval reads inside the TTL do not issue another `eth_call`.
- `DELETE /api/prediction/approve/cache?owner=...` clears cache entries for one owner; omitting owner clears all approval caches.
- Expired entries are refreshed from RPC before order validation uses them.

## RPC Transaction Tracking Baseline

Backend-observed effectful RPC transactions use `RpcTransactionTrackingService` before any future relayer or backend submit path treats a transaction as durable.

- `commandId` is the idempotency key for the effectful RPC command.
- `fingerprint` identifies the intended chain effect, such as owner/spender/token/amount for approval-like flows.
- Same `commandId`, `fingerprint`, and `txHash` replays the existing transaction record.
- Same `commandId` with a different `fingerprint` or `txHash` is rejected as an idempotency conflict.
- `V10__rpc_transaction_records.sql` persists command, chain, wallet, transaction type, fingerprint, transaction hash, status, and completion state.
- Operators can inspect unresolved submitted transactions with `GET /api/prediction/approve/rpc-transactions/unresolved?limit=100`.
