# Phase 10 Audit

## Scope

This audit was performed on 2026-07-06 against the current repository state using a production exchange standard:

- real user funds
- real order execution
- real settlement
- real wallet movement
- real reconciliation

Interfaces, DTOs, stubs, mocks, placeholders, and TODO markers are not counted as completed production capability.

## Executive Summary

- Phase 1 through Phase 8 delivered frontend routes, components, layout, i18n, and mock/development adapters.
- Phase 9 delivered the Java server skeleton, asset-domain contracts, and a validation-only transfer stub.
- Phase 10 delivered Java wallet, market data, spot, and Open API records/interfaces/stubs.
- The repo does not contain a production matching engine, a production order book, a reservation engine, a double-entry ledger engine, a settlement engine, real deposit/withdrawal chain integration, reconciliation jobs, an admin operations backend, or production deployment readiness.

## What Phase 1-10 Actually Completed

| Phase | Actual deliverables in repo | Production meaning |
| --- | --- | --- |
| 1 | `web/` React + TypeScript + Vite scaffold, entrypoints, build scripts | Frontend project foundation only |
| 2 | App shell, layout, header, sidebar, shared UI components, route structure | UI framework only |
| 3 | Login/register/reset/2FA/home/markets pages plus `mockAuthService` and `mockMarketService` | Public/account UI with mock data |
| 4 | Account center pages plus `mockAccountService` | Account UI only |
| 5 | Asset/transfer/deposit/withdraw pages plus `mockAssetService` and `mockWalletService` | Asset UI only, not real wallet logic |
| 6 | Spot/futures/margin trading pages plus `mockTradingService` | Trading UI only, not real order flow |
| 7 | Orders/positions/API key/notification pages plus `mockPhase7Service` | User-center trading UI only |
| 8 | Admin console pages plus `mockAdminService` and mock admin auth | Admin UI only |
| 9 | `server/` Spring Boot skeleton, `account`, `ledger`, `idempotency`, `DefaultAccountTransferService` | Domain contracts and validation stub only |
| 10 | `wallet`, `market`, `spot`, and `openapi` Java records/interfaces/stubs | Service boundaries and placeholders only |

Additional frontend subphases such as 7.5 and 7.6 improved i18n and layout quality, but they did not add production trading or production funds logic.

## Stub, Interface, Mock, and Placeholder Inventory

### Frontend mock or adapter layers

- `web/src/features/trading/mockTradingService.ts`
- `web/src/features/assets/mockAssetService.ts`
- `web/src/features/assets/wallet/mockWalletService.ts`
- `web/src/features/account/mockAccountService.ts`
- `web/src/features/auth/mockAuthService.ts`
- `web/src/features/phase7/mockPhase7Service.ts`
- `web/src/admin/features/console/mockAdminService.ts`
- `web/src/admin/auth/mockAdminAuthService.ts`

### Java interfaces only

- `server/src/main/java/com/lumix/ledger/LedgerService.java`
- `server/src/main/java/com/lumix/idempotency/IdempotencyService.java`
- `server/src/main/java/com/lumix/spot/MatchingEngineClient.java`
- `server/src/main/java/com/lumix/spot/SpotOrderService.java`
- `server/src/main/java/com/lumix/wallet/WalletService.java`
- `server/src/main/java/com/lumix/wallet/WalletGateway.java`
- `server/src/main/java/com/lumix/wallet/DepositService.java`
- `server/src/main/java/com/lumix/wallet/WithdrawService.java`
- `server/src/main/java/com/lumix/market/MarketDataService.java`
- `server/src/main/java/com/lumix/market/PriceIndexService.java`
- `server/src/main/java/com/lumix/market/MarkPriceService.java`

### Validation or fail-closed stubs

- `server/src/main/java/com/lumix/account/DefaultAccountTransferService.java`
- `server/src/main/java/com/lumix/spot/DefaultSpotOrderService.java`
- `server/src/main/java/com/lumix/wallet/DefaultDepositService.java`
- `server/src/main/java/com/lumix/wallet/DefaultWithdrawService.java`
- `server/src/main/java/com/lumix/market/DefaultMarketDataService.java`
- `server/src/main/java/com/lumix/market/DefaultPriceIndexService.java`
- `server/src/main/java/com/lumix/market/DefaultMarkPriceService.java`
- `server/src/main/java/com/lumix/openapi/DefaultApiSignatureVerifier.java`
- `server/src/main/java/com/lumix/openapi/DefaultApiRateLimitService.java`
- `server/src/main/java/com/lumix/openapi/OpenApiRouteRegistry.java` for metadata only

### Missing production layers

- No `core/` or `matching-core/` directory
- No database migrations
- No repositories
- No REST controllers
- No queue consumers or producers
- No websocket market-data broadcaster
- No wallet gateway implementation
- No ledger implementation
- No idempotency implementation
- No settlement implementation
- No reconciliation implementation

## Direct Answers To The Required Audit Questions

### 1. Where is the matching engine and does production matching exist?

- The only matching-related server contract is `server/src/main/java/com/lumix/spot/MatchingEngineClient.java`.
- It is an interface only.
- No implementation of `MatchingEngineClient` exists.
- No `core/` or `matching-core/` source tree exists.
- `DefaultSpotOrderService` explicitly does not submit to matching.

Conclusion: production matching does not exist in the repo.

### 2. Does asset reservation, locked balance, or fund freeze exist as a production implementation?

- `AccountBalanceView` and `AssetAccountView` expose `total`, `available`, and `locked` as read models only.
- `LedgerService` declares `reserve`, `release`, `commit`, and `rollback`.
- No implementation of `LedgerService` exists.
- No persistence, concurrency control, or idempotent reservation lifecycle exists.
- No service in the repo calls a real reserve/release/commit/rollback path.

Conclusion: production reservation or fund freeze does not exist in the repo.

### 3. Does the ledger have real double-entry balance mutation?

- `LedgerPostingRequest`, `LedgerJournalRequest`, `LedgerEntryDirection`, and `LedgerJournalResult` define data shapes only.
- No ledger engine implementation exists.
- No balance projection exists.
- No double-entry journal balancing logic exists.
- No database schema exists for journals, balances, or postings.
- `DefaultAccountTransferService` intentionally returns `PENDING_LEDGER_REVIEW` without posting.

Conclusion: no double-entry mutation engine exists in the repo.

### 4. Does the spot order flow really validate, freeze, persist, submit, fill, settle, and release?

| Step | Repo status | Notes |
| --- | --- | --- |
| Validate order | Partial only | `DefaultSpotOrderService` checks quantity, limit price, and `timeInForce` basics |
| Freeze asset | No | Only TODO comments mention future reserve behavior |
| Persist order | No | No repository, DB schema, or storage call exists |
| Submit to matching engine | No | Service explicitly does not call `matchingEngineClient.submitOrder(...)` |
| Receive fill | No | No event consumer or fill handler exists |
| Settle trade | No | No settlement engine exists |
| Release unused funds | No | Cancel path is a placeholder and always returns `false` |

Conclusion: production spot order orchestration does not exist.

### 5. Are deposit and withdraw real on-chain operations?

- Deposit:
  - `DefaultDepositService` validates a `DepositRecord`.
  - It intentionally does not call `IdempotencyService.startProcessing(...)`.
  - It intentionally does not call `LedgerService.postJournal(...)`.
  - There is no callback endpoint, chain watcher, or wallet gateway implementation.
- Withdraw:
  - `DefaultWithdrawService` validates a `WithdrawRecord`.
  - It intentionally does not call `LedgerService.reserve(...)`, `release(...)`, or `commit(...)`.
  - It intentionally does not call `WalletGateway.submitWithdrawal(...)`.
  - There is no security enforcement, risk approval pipeline, or chain broadcaster.

Conclusion: real deposit and real withdrawal do not exist.

### 6. Is market data generated from matching events?

- `DefaultMarketDataService.getDepth(...)` returns an empty `OrderBookSnapshot`.
- `getRecentTrades(...)` returns an empty list.
- `getTicker24h(...)` returns `Optional.empty()`.
- `getKline(...)` returns an empty list.
- No event bus, cache, websocket, or trade-event consumer exists.

Conclusion: market data is not generated from matching events.

## Production Gap Register

The following core exchange capabilities are still absent:

- Production matching engine
- Deterministic order book
- Reservation engine
- Double-entry ledger
- Trade settlement
- Deposit ingestion and wallet credit
- Withdrawal approval and chain broadcast
- Reconciliation and compensation
- Admin operations backend
- Risk controls
- Market data pipeline
- Production deployment readiness

## Reset Outcome

Because the repo only contains UI mocks and Java stubs after Phase 10, the next valid step is a production architecture reset, followed by a production schema and implementation roadmap. That reset is captured in:

- `docs/PRODUCTION_ROADMAP.md`
- `docs/ARCHITECTURE_PRODUCTION.md`
- `docs/TRADING_CORE_BOUNDARIES.md`
- `docs/FUNDS_SAFETY_MODEL.md`
- `docs/ORDER_SETTLEMENT_FLOW.md`
