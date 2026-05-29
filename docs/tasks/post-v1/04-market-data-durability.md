# Task: Market Data Durability

Status: `todo`

## Goal

Persist market data sequences, depth deltas, ticker, kline, and trade tape enough to support restart recovery and reconnect backfill.

## Scope

- Durable sequence checkpoints per symbol and stream.
- Snapshot and delta persistence strategy.
- Reconnect backfill API or service boundary.
- Ticker, kline, and trade tape retention.
- Retention and archive policy for high-volume market data.

## First Implementation Slice

1. Persist depth delta sequence checkpoints for one symbol stream.
2. Read the latest checkpoint during market-data recovery.
3. Add tests for monotonic sequence advance, duplicate delta handling, and recovery from the latest checkpoint.
4. Comment the tests around snapshot-plus-delta assumptions and checksum expectations.

## Acceptance Criteria

- Depth stream recovery can discover the latest durable sequence checkpoint.
- Duplicate or out-of-order sequence writes are rejected or ignored deterministically.
- The market-data map documents restart and reconnect behavior.

## Read First

- [../../ai/maps/reliability-market-data.md](../../ai/maps/reliability-market-data.md)
- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
