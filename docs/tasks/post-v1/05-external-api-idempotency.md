# Task: External API Idempotency

Status: `doing`

## Goal

Verify and enforce timeout, retry, circuit breaker, rate limit, and idempotency coverage for external APIs so retries cannot create duplicate external effects.

## Scope

- Gamma, CLOB, RPC, hedge venue, and bank/chain callback clients.
- Idempotency keys and request identity storage.
- Retry policies that distinguish safe reads from effectful writes.
- Circuit breaker and rate limit defaults per integration.
- Audit and reconciliation hooks for uncertain external outcomes.

## First Implementation Slice

1. [x] Inventory all external clients and classify calls as read-only, idempotent write, or non-idempotent write.
2. [x] Add an idempotency/retry envelope for one high-risk effectful command.
3. [x] Add tests for timeout, retry, duplicate request key, and uncertain remote outcome.
4. [x] Comment tests around why each retry is safe or blocked.

## Progress

- Added `docs/en/external-api-idempotency.md` and `docs/zh-TW/external-api-idempotency.md` with Gamma, CLOB, RPC/approval, hedge venue, and callback inventory.
- Added `IdempotentHedgeVenueAdapter` as the first effectful-write envelope. It requires `HedgeOrderRequest.refId`, fingerprints payloads, claims before venue submit, returns durable terminal results for duplicate requests, rejects same-key/different-payload conflicts, and blocks duplicate submit after pending or timeout-like uncertain outcomes.
- Added `IdempotentHedgeVenueAdapterTest` covering accepted duplicate replay, conflict, uncertain outcome, and missing ref id behavior.
- Added `HedgeVenueIdempotencyStore`, `JpaHedgeVenueIdempotencyStore`, and `V8__hedge_venue_idempotency_records.sql` so hedge venue claim/result records survive process restart.
- Added optional `PolymarketPlaceOrderRequest.clientRequestId` so CLOB place can use the local order record as an idempotency boundary before session limit consumption, approval checks, signing, or `/order` calls.
- Added `PolymarketOrderServiceTest` covering same-key duplicate replay, same-key payload conflict, and existing local order with uncertain CLOB outcome.
- Added a CLOB cancel local idempotency baseline: once cancel records `CANCEL_REQUESTED`, `CANCEL_OUTCOME_UNCERTAIN`, or a canceled terminal status, duplicate cancel requests return the local order without another CLOB DELETE.
- Added `PolymarketClobCommandStore`, `JpaPolymarketClobCommandStore`, and `V9__polymarket_clob_command_records.sql` so CLOB cancel can use a durable `commandId` claim/result record.
- Added `PolymarketOrderTrackingServiceTest` covering cancel duplicate replay, commandId replay/conflict, first successful cancel marker, exception/5xx uncertain outcomes, reconcile resolution for uncertain cancel, and unchanged sync/reconcile replay.
- Added `PolymarketApprovalServiceTest` covering approval read cache hits, owner-scoped cache clear, and TTL refresh before order validation relies on RPC approval state.
- Added CLOB sync/reconcile local no-op replay: unchanged CLOB payload/status/size/error does not save the local order row again, and reconcile reports unchanged rows separately.

Remaining work:
- Add venue lookup/reconciliation for real hedge adapters and operator handling for uncertain outcomes.
- Add fuller local state-machine coverage for CLOB/trade/settlement transitions.
- Add RPC approval transaction idempotency tracking for any future backend-observed effectful approval/relayer flow.

## Acceptance Criteria

- External effectful writes have an idempotency story before retries are enabled.
- Timeout/retry/circuit/rate-limit coverage is documented per client.
- Tests prove duplicate external commands do not create duplicate local effects.

## Read First

- [../../ai/maps/polymarket-security.md](../../ai/maps/polymarket-security.md)
- [../../ai/maps/market-maker-hedging.md](../../ai/maps/market-maker-hedging.md)
- [../../ai/maps/reliability-market-data.md](../../ai/maps/reliability-market-data.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
