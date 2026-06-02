<!-- File purpose: live position SQL mirror/index decision. Chinese version: ../zh-TW/live-position-sql-mirror.md. -->
# Live Position SQL Mirror

This document closes the P1 live-position SQL mirror/index design decision.

## Decision

Add a dedicated durable live-position projection in a later implementation slice. Do not use `account_risk_snapshots` as the live-position mirror.

Why:
- Redis `pos:{uid}` and `pos:open:index` are still the current low-latency hot state for individual positions.
- `account_risk_snapshots` stores account-level aggregates such as margin, equity, risk ratio, and open-position count. It does not store per-symbol quantity, entry price, realized PnL, or side.
- Liquidation scanning, ADL, funding, and market-maker exposure all need per-position rows with deterministic open-position indexes.

## Proposed Table

Future migration name:

```text
V{next}__position_lifecycle_projection.sql
```

Table:

```text
position_lifecycle_projection
```

Required columns:

| Column | Purpose |
| --- | --- |
| `uid` | User id. |
| `symbol` | Trading symbol. |
| `mode` | Margin mode, such as `CROSS` or `ISOLATED`. |
| `leverage` | Current leverage. |
| `qty` | Signed position quantity; zero means closed. |
| `entry_price` | Weighted average entry price. |
| `margin` | Isolated margin or allocated cross-position margin. |
| `realized_pnl` | Cumulative realized PnL. |
| `fee_paid` / `rebate_earned` | Cumulative fee and rebate fields. |
| `funding_paid` / `funding_received` | Cumulative funding fields. |
| `insurance_fund_covered` / `adl_covered` | Liquidation shortfall coverage fields. |
| `last_trade_ref` | Last internal trade, match, liquidation, funding, or ADL reference that changed the position. |
| `updated_at` | Latest projection update time. |

Primary key:

```text
(uid, symbol)
```

Required indexes:

| Index | Query |
| --- | --- |
| `(symbol, qty, updated_at)` | liquidation/ADL/open-position scans by symbol. |
| `(uid, updated_at)` | user position screens and account restore checks. |
| `(updated_at)` | operator drift and stale-projection scans. |

Use a generated `is_open` boolean only if MySQL query plans cannot use `qty <> 0` efficiently at production cardinality.

## Rebuild Source

The projection must be rebuildable from durable state:

1. Matching trade events for fills and signed quantity movement.
2. Wallet ledger journal rows for fees, rebates, funding, liquidation shortfall, insurance, and ADL coverage.
3. Durable ADL/liquidation execution records for command idempotency and operator audit references.
4. Account risk snapshots only as validation evidence, not as the per-position rebuild source.

Redis repair rule:

1. Rebuild `position_lifecycle_projection` first.
2. Rebuild Redis `pos:{uid}` and `pos:open:index` from non-zero SQL projection rows.
3. Do not rerun original trade/liquidation/funding commands to repair Redis drift.

## Acceptance Gate For Implementation

The implementation slice should add:

- Flyway migration for `position_lifecycle_projection`.
- JPA entity/repository or a store adapter.
- Projection update path from trade/funding/liquidation/ADL state changes.
- A rebuild or reconciliation service that compares Redis open positions against SQL projection rows.
- Focused tests for open-position scan, zero-quantity close/removal, Redis rebuild source, and account-position consistency.

Until that implementation lands, Redis remains the serving hot state and the SQL mirror remains a documented production-hardening backlog item.
