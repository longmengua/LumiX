# Web Apps Map

This map covers future client-facing and admin-facing web applications.

## Existing Frontend State

- Current static page: `src/main/resources/static/index.html`
- Current client exchange console: `src/main/resources/static/exchange.html`
- Current admin test-funds page: `src/main/resources/static/admin-test-funds.html`
- Current static admin page: `src/main/resources/static/admin-market-config.html`
- Current static admin risk page: `src/main/resources/static/admin-risk-parameters.html`
- Current static admin market-maker page: `src/main/resources/static/admin-market-maker.html`
- Current static admin DLQ page: `src/main/resources/static/admin-dlq.html`
- Current static README: `src/main/resources/static/README.md`
- Browser smoke tests: `tests/e2e/static-admin-pages.spec.js`
- E2E runner: `shells/e2e-browser.sh`
- There is no full production frontend app yet.

## Client Web Scope

- Trading dashboard: order book, ticker, trades, klines, order entry.
- Account dashboard: balances, frozen funds, margin risk, ledger, transfers.
- Order workflows: place, amend, cancel, cancel-replace, bulk cancel.
- User notifications: order lifecycle, fills, funding, liquidation, risk alerts.
- Auth: first-party register/login/logout, API key/JWT-compatible flow without committing secrets, and deferred third-party OAuth/passkey/wallet login.

## Admin Web Scope

- Operations dashboard: metrics, matching status, Kafka/outbox/DLQ, reconciliation.
- Risk controls: global switches, symbol suspension, risk parameters.
- Finance operations: ledger replay, reconciliation reports, exception workflow.
- Recovery operations: snapshot/replay, DLQ replay, manual compensation.
- Market-maker operations: inventory, kill switch, hedge status, audit trail.

Implemented baseline:
- `exchange.html` is the client exchange console for first-party auth, admin-configured market selection, depth, order entry, open orders, and account lookup through `/api/auth`, `/api/markets`, `/api/depth`, `/api/order`, and `/api/margin`; client UID is derived from the authenticated session rather than editable form input. The order book renders animated depth bars and a Market Maker Flow panel that reads `/api/market-maker/quotes/active` to mark visible maker quote legs; the page uses one `/ws/exchange` multiplex WebSocket, sends `subscribe.market` before login, adds `subscribe.user` after login, reacts to `market-maker.quote` / order lifecycle / trade signals, supports opt-in cancel-on-disconnect resume metadata, and falls back to one-second polling while the exchange stream is disconnected or reconnecting.
- `admin-test-funds.html` is the admin MVP funding page for issuing test funds through `/api/admin/test-funds/airdrop`; this keeps privileged operator actions separate from the client trading workflow.
- `AdminMarketConfigController` exposes `/api/admin/market-config` market configuration data and audited fee updates through `POST /api/admin/market-config/{symbol}/fees`; the static admin market-config page explains that fee edits apply only to new orders because existing orders carry fee snapshots.
- `AdminRiskParametersController` exposes read-only `/api/admin/risk-parameters` risk switches, symbol tiers, and oracle state for the static admin risk-parameters page; write actions remain disabled until permissioned backend endpoints exist.
- `admin-market-maker.html` exposes the existing `/api/market-maker` operator surface for creating/updating market-maker profiles, per-symbol strategy limits, quote state inspection, hedge reconciliation, hedge fills, idempotency reports, and guarded manual hedge execution.
- `AdminDlqController` exposes read-only `/api/admin/dlq` DLQ rows with sanitized payload/header previews for the static admin DLQ page; replay/compensation actions remain disabled in UI pending permissioned operator workflow wiring.

## Design Constraints

- Operational screens should be dense, restrained, and optimized for scanning.
- Prefer read-only views before adding mutating controls.
- Mutating admin actions need confirmation, audit context, and trace id display.
- Frontend must not contain private keys, API secrets, or long-lived privileged tokens.
- Static page changes should include Playwright E2E smoke coverage for page load, core controls, mocked API render paths, and mobile/desktop viewport sanity.

## Likely Backend Areas

- Order APIs: `interfaces.web.controller.OrderController`
- Margin/account APIs: `interfaces.web.controller.MarginController`
- Market data APIs: `DepthController`, `MarketDataController`
- Risk APIs: `RiskController`
- Recovery/admin APIs: `RecoveryController`, `OperationsController`
- Security: `ApiAuthenticationInterceptor`, `ProtectedApiSecurityInterceptor`
