# Web Apps Map

This map covers future client-facing and admin-facing web applications.

## Existing Frontend State

- Current static page: `src/main/resources/static/index.html`
- Current client exchange console: `src/main/resources/static/exchange.html`
- Current admin test-funds page: `src/main/resources/static/admin-test-funds.html`
- Current static admin page: `src/main/resources/static/admin-market-config.html`
- Current static admin risk page: `src/main/resources/static/admin-risk-parameters.html`
- Current static admin market-maker page: `src/main/resources/static/exchange-admin.html`
- Current static admin DLQ page: `src/main/resources/static/admin-dlq.html`
- Current static README: `src/main/resources/static/README.md`
- Browser smoke tests: `tests/e2e/static-admin-pages.spec.js`, `tests/e2e/exchange-order-entry-smoke.js`
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
- `exchange.html` is the prod-facing client exchange console for first-party auth, admin-configured market selection in the order-entry form, depth, order entry, open orders, account lookup, and profile drawer snapshots through `/api/auth`, `/api/markets`, `/api/depth`, `/api/order`, and `/api/margin`; client UID is derived from the authenticated session rather than editable form input. The main surface keeps only the Trade tab compact; sign-in, registration, asset summary, and logout live in the top-right profile drawer. Before auth the drawer shows a standard email/password sign-in card, reads `/api/auth/config`, can render a Cloudflare Turnstile-compatible free human-verification challenge, and treats registration as a pending email-verification flow with a six-digit email code entry as the primary path plus a direct verification link as backup; pending registrations can call `/api/auth/resend-verification` to rotate the code/link without extending the original 24-hour expiry. Backend registration keeps pending account material and backup link tokens in `customer_registration_requests`; six-digit codes live in independent `customer_verification_codes` rows keyed by email/account so admin resend tooling can reuse them later without binding codes to one feature. After auth it shows one account asset summary, open orders, held positions, and position history without customer-facing section toggles. The page is split into `exchange.html`, `css/exchange.css`, and `js/exchange.js`. The order book renders public market depth bars with independent ask/bid tick depth controls for 5, 10, 20, or 50 ticks per side, and it must not expose market-maker telemetry, privileged admin links, checksum, book version diagnostics, raw auth/account debug logs, or a customer-facing market-data reload button; the page uses one `/ws/exchange` multiplex WebSocket, sends `subscribe.market` before login, adds `subscribe.user` after login, reacts to public market / order lifecycle / trade signals, supports opt-in cancel-on-disconnect resume metadata, and falls back to one-second polling while the exchange stream is disconnected or reconnecting.
- `admin-market-config.html` is where operators inspect market-data diagnostics such as best bid/ask, book version, and checksum.
- `exchange.html`, `admin-console.html`, and `exchange-admin.html` share the `localStorage.exchangeLanguage` locale preference for English, Traditional Chinese, Bahasa Malaysia, and Korean; English remains the default for first-load demos and browser tests.
- `exchange.html` is the prod-facing customer trading entry and must not expose links to privileged admin pages; operator navigation starts from `admin-console.html`.
- `admin-test-funds.html` is the admin MVP funding page for issuing test funds through `/api/admin/test-funds/airdrop`; this keeps privileged operator actions separate from the client trading workflow.
- `AdminMarketConfigController` exposes `/api/admin/market-config` market configuration data and audited fee updates through `POST /api/admin/market-config/{symbol}/fees`; the static admin market-config page explains that fee edits apply only to new orders because existing orders carry fee snapshots.
- `AdminRiskParametersController` exposes read-only `/api/admin/risk-parameters` risk switches, symbol tiers, and oracle state for the static admin risk-parameters page; write actions remain disabled until permissioned backend endpoints exist.
- `exchange-admin.html` exposes the existing `/api/market-maker` operator surface for creating/updating market-maker profiles, per-symbol strategy limits, auto quote status/run-once diagnostics, quote state inspection, hedge reconciliation, hedge fills, idempotency reports, and guarded manual hedge execution; `admin-console.html` opens it as an embedded Market Makers tab with `?embed=1` so operators stay in one admin screen, while the direct URL remains a compatibility entry point.
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
