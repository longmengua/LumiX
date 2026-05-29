# Task: External API Idempotency

Status: `todo`

## Goal

Verify and enforce timeout, retry, circuit breaker, rate limit, and idempotency coverage for external APIs so retries cannot create duplicate external effects.

## Scope

- Gamma, CLOB, RPC, hedge venue, and bank/chain callback clients.
- Idempotency keys and request identity storage.
- Retry policies that distinguish safe reads from effectful writes.
- Circuit breaker and rate limit defaults per integration.
- Audit and reconciliation hooks for uncertain external outcomes.

## First Implementation Slice

1. Inventory all external clients and classify calls as read-only, idempotent write, or non-idempotent write.
2. Add an idempotency/retry envelope for one high-risk effectful command.
3. Add tests for timeout, retry, duplicate request key, and uncertain remote outcome.
4. Comment tests around why each retry is safe or blocked.

## Acceptance Criteria

- External effectful writes have an idempotency story before retries are enabled.
- Timeout/retry/circuit/rate-limit coverage is documented per client.
- Tests prove duplicate external commands do not create duplicate local effects.

## Read First

- [../../ai/maps/polymarket-security.md](../../ai/maps/polymarket-security.md)
- [../../ai/maps/market-maker-hedging.md](../../ai/maps/market-maker-hedging.md)
- [../../ai/maps/reliability-market-data.md](../../ai/maps/reliability-market-data.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
