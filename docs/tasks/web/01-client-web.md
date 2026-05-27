# Task: Client Web

Status: `todo`

## Goal

Build a client-facing web application for trading, account, market data, positions, orders, transfers, risk snapshots, and user notifications.

## Scope

- Trading screen with order entry, order book, recent trades, ticker, positions, open orders, and order history.
- Account screen with balance, available/frozen funds, margin mode, ledger, transfers, and risk ratio.
- Deposit/withdrawal and transfer state views.
- Market data views for depth, ticker, trades, and klines.
- User event stream for order lifecycle, fills, cancel-on-disconnect, liquidation, funding, and alerts.
- Authentication flow using the existing API auth/JWT model.
- API error handling with trace id display.

## First Implementation Slice

1. Decide frontend location and stack based on the repository state.
2. Add a web app shell with routing for trading, account, orders, and settings.
3. Add typed API client wrappers for existing exchange endpoints.
4. Implement a read-only trading dashboard before enabling order submission.
5. Add tests or smoke checks for the main UI routes.

## Acceptance Criteria

- Client can inspect market data, balances, positions, open orders, and ledger state.
- Order placement is guarded by clear validation and API error feedback.
- The UI exposes request/trace ids when backend calls fail.
- No secret is committed to frontend config.

## Read First

- [../../ai/maps/web-apps.md](../../ai/maps/web-apps.md)
- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
