# Task: Market Data Durability

Status: `done`

## Goal

Persist market data sequences, depth deltas, ticker, kline, and trade tape enough to support restart recovery and reconnect backfill.

## Scope

- Durable sequence checkpoints per symbol and stream.
- Snapshot and delta persistence strategy.
- Reconnect backfill API or service boundary.
- Ticker, kline, and trade tape retention.
- Retention and archive policy for high-volume market data.

## First Implementation Slice

1. [x] Persist depth delta sequence checkpoints for one symbol stream.
2. [x] Read the latest checkpoint during market-data recovery.
3. [x] Add tests for monotonic sequence advance, duplicate delta handling, and recovery from the latest checkpoint.
4. [x] Comment the tests around snapshot-plus-delta assumptions and checksum expectations.

## Progress

- Added `MarketDataSequenceCheckpoint`, `MarketDataSequenceCheckpointStore`, `MarketDataSequenceCheckpointService`, JPA record/adapter, and Flyway migration `V3__market_data_sequence_checkpoints.sql`.
- `MarketDataService` now initializes depth-delta version from the durable `DEPTH_DELTA` checkpoint when configured.
- Each generated depth delta advances the durable checkpoint with sequence and checksum.
- Duplicate or out-of-order checkpoint writes are ignored deterministically by the service.
- Added durable depth delta storage and `GET /api/market-data/{symbol}/depth-deltas?afterVersion=...` for reconnect backfill.
- Added durable trade tape storage so recent trades survive service restart when the store is configured.
- Added durable ticker latest-state storage so the current ticker survives service restart when the store is configured.
- Added durable 1m kline storage so candlestick reads survive service restart when the store is configured.
- Added disabled-by-default `MarketDataRetentionScheduler` and `MarketDataRetentionService` with independent retention windows for depth deltas, trade tape, and 1m klines.
- Added focused tests for monotonic checkpoint advance, duplicate/out-of-order handling, restoring depth version from the latest checkpoint before emitting the next delta, backfilling deltas after a known version, reading recent trades from durable tape after service restart, reading ticker latest state from durable storage after service restart, reading 1m kline OHLCV from durable storage after service restart, and retention cutoff behavior.

Remaining work:
- Production archive export/storage is still a broader ops task; this slice defines and enforces DB retention windows.

## Acceptance Criteria

- Depth stream recovery can discover the latest durable sequence checkpoint.
- Duplicate or out-of-order sequence writes are rejected or ignored deterministically.
- The market-data map documents restart and reconnect behavior.

## Read First

- [../../ai/maps/reliability-market-data.md](../../ai/maps/reliability-market-data.md)
- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
