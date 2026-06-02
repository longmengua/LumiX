# Task: Market Data Fanout Load Test

Status: `todo`

## Goal

Add a repeatable load test spec and tool entry point for market-data SSE/WebSocket fanout, heartbeat behavior, and reconnect recovery.

## Scope

- Simulate many market-data clients subscribing to depth, trade, ticker, and private user streams where appropriate.
- Capture connection count, subscription rate, heartbeat misses, message lag, reconnect success, and dropped message categories.
- Exercise recovery cursor behavior for depth/trade streams.
- Keep private stream tests scoped to synthetic accounts and non-production credentials.

## First Implementation Slice

1. Choose load-test runtime and config format.
2. Add a dry-run/config-validation mode.
3. Add fanout profile fields for client count, symbols, stream types, reconnect cadence, and duration.
4. Define report output and failure thresholds.
5. Add docs for required non-production environment setup.

## Acceptance Criteria

- Test profiles cannot target production by default.
- Report includes fanout lag, heartbeat misses, reconnect success rate, and subscription errors.
- Recovery cursor coverage is included or explicitly marked pending with backend dependency.

## Read First

- [../../ai/maps/reliability-market-data.md](../../ai/maps/reliability-market-data.md)
- [../../en/market-data-gateway-scaling.md](../../en/market-data-gateway-scaling.md)

