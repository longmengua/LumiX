<!-- File purpose: External API retry/idempotency inventory. Chinese version: ../zh-TW/external-api-idempotency.md. -->
# External API Idempotency

## Inventory

| Integration | Current client | Operation type | Retry/circuit/rate limit | Idempotency status |
| --- | --- | --- | --- | --- |
| Gamma market API | `PredictionGammaMarketClient` | Read-only market discovery | Shared OkHttp timeout/retry/circuit/rate-limit baseline | Safe to retry as read-only; response schema versioning still TODO. |
| Polymarket CLOB place/cancel | `PolymarketClobTradingClient` via `PolymarketOrderService` | External effectful writes | Shared OkHttp baseline where HTTP client is used | Still TODO: local/CLOB state machine and idempotent place/cancel/reconcile commands. |
| Polymarket relayer/RPC approval | `PolymarketSessionService` / Web3j config | External effectful writes and reads | Shared config partly applies; RPC-specific limits still TODO | Still TODO: approval cache, expiry, and idempotent transaction tracking. |
| Hedge venue submit | `HedgeVenueAdapter` | External effectful write | `RetryingHedgeVenueAdapter`, `ThrottlingHedgeVenueAdapter` | Baseline: `IdempotentHedgeVenueAdapter` requires `refId`, returns cached terminal results, rejects refId conflicts, and blocks duplicate submits after uncertain timeout-like outcomes. |
| Bank/chain callbacks | Future callback clients/controllers | External effectful reconciliation | Not implemented | Still TODO. |

## Hedge Submit Baseline

Hedge venue submit uses `HedgeOrderRequest.refId` as the external idempotency key. The idempotency decorator stores a fingerprint of the first payload for each `refId`.

- Same `refId` and same payload after an accepted or non-retryable result returns the stored result without another venue call.
- Same `refId` with different payload is rejected with `HEDGE_VENUE_IDEMPOTENCY_CONFLICT`.
- Same `refId` after a retryable/timeout-like result is blocked with `HEDGE_VENUE_OUTCOME_UNCERTAIN`, because the venue may have received the first request.
- Missing `refId` is rejected before any venue call.

This is an in-process baseline for the current MVP. A production venue adapter still needs durable idempotency storage, venue order lookup/reconciliation, operator handling for uncertain outcomes, and integration-specific rate limits.
