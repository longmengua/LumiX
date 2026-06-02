# Web Apps Map

This map covers future client-facing and admin-facing web applications.

## Existing Frontend State

- Current static page: `src/main/resources/static/index.html`
- Current static admin page: `src/main/resources/static/admin-market-config.html`
- Current static README: `src/main/resources/static/README.md`
- There is no full production frontend app yet.

## Client Web Scope

- Trading dashboard: order book, ticker, trades, klines, order entry.
- Account dashboard: balances, frozen funds, margin risk, ledger, transfers.
- Order workflows: place, amend, cancel, cancel-replace, bulk cancel.
- User notifications: order lifecycle, fills, funding, liquidation, risk alerts.
- Auth: API key/JWT-compatible flow without committing secrets.

## Admin Web Scope

- Operations dashboard: metrics, matching status, Kafka/outbox/DLQ, reconciliation.
- Risk controls: global switches, symbol suspension, risk parameters.
- Finance operations: ledger replay, reconciliation reports, exception workflow.
- Recovery operations: snapshot/replay, DLQ replay, manual compensation.
- Market-maker operations: inventory, kill switch, hedge status, audit trail.

Implemented baseline:
- `AdminMarketConfigController` exposes read-only `/api/admin/market-config` market configuration data for the static admin market-config page; write actions remain disabled until permissioned backend endpoints exist.

## Design Constraints

- Operational screens should be dense, restrained, and optimized for scanning.
- Prefer read-only views before adding mutating controls.
- Mutating admin actions need confirmation, audit context, and trace id display.
- Frontend must not contain private keys, API secrets, or long-lived privileged tokens.

## Likely Backend Areas

- Order APIs: `interfaces.web.controller.OrderController`
- Margin/account APIs: `interfaces.web.controller.MarginController`
- Market data APIs: `DepthController`, `MarketDataController`
- Risk APIs: `RiskController`
- Recovery/admin APIs: `RecoveryController`, `OperationsController`
- Security: `ApiAuthenticationInterceptor`, `ProtectedApiSecurityInterceptor`
