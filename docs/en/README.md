<!-- File purpose: English project overview. Other languages are listed in the repository root README.md. -->
# Java21 Match Hub

Java21 Match Hub is a Java 21 + Spring Boot 3.5 backend for an exchange core and Polymarket integration. The project currently has two business lines: an internal exchange core and a Polymarket prediction-market integration. The codebase follows a DDD-style layered structure and uses MySQL, Redis, and Kafka as local development infrastructure.

This is a runnable trading-core MVP, not a production-grade exchange. The matching engine is still in-memory, and the local Kafka, Redis, and MySQL setup is single-node. Before production, the system still needs durable matching, stronger risk controls, ledger auditability, key management, observability, and load testing.

Other languages: [repository language directory](../../README.md) / [繁體中文](../zh-TW/)

## Documentation Category

This page is the English **Product Documentation** entry.

| Category | Description | Link |
| --- | --- | --- |
| Product Documentation | Business-facing overview, features, modules, APIs, and order placement flow. | Current page |
| Current State | Quick snapshot of completion level, MVP baseline, production blockers, and near-term priorities. | [current-state.md](current-state.md) |
| Core V1 Release | Freeze scope, release gate, smoke runbook, and verification commands for the bounded core-v1 baseline. | [core-v1-release-checklist.md](core-v1-release-checklist.md) / [core-v1-smoke-runbook.md](core-v1-smoke-runbook.md) |
| Technical Documentation | Architecture, implementation notes, API curl scripts, and matching engine notes. | [technical.md](technical.md) |
| TODO Documentation | Production-readiness roadmap grouped by priority and domain. | [todo.md](todo.md) |
| AI Documentation | Compact Codex/agent maps and task-entry workflow. | [ai.md](ai.md) |
| Task Documentation | Selectable task files for roadmap and interrupt work. | [tasks.md](tasks.md) |

## Tech Stack

- Java 21
- Spring Boot 3.5.6
- Spring Web / Validation / WebSocket
- Spring Data JPA + MySQL 8.4
- Redis 7.4
- Kafka 7.6 KRaft single node
- Flyway
- OkHttp / Web3j
- Lombok
- Maven Wrapper

## Local Startup

Start local dependencies first:

```bash
docker compose up -d
```

Docker Compose starts:

| Service | Port | Purpose |
| --- | ---: | --- |
| MySQL | 3306 | JPA entities, Polymarket markets, sessions, and orders |
| Redis | 6379 | Low-latency state, snapshots, idempotency, outbox, DLQ |
| Kafka | 9092 | Domain events, event store, Polymarket user events |
| Kafka UI | 8081 | Local topic, message, and consumer-group inspection |

Start the Spring Boot app:

```bash
./mvnw spring-boot:run
```

The default profile is `dev`, which connects to `localhost:3306`, `localhost:6379`, and `localhost:9092`. The application API is exposed at `http://localhost:8080`.

Useful local commands:

```bash
docker compose ps
docker compose logs -f kafka
docker compose down
docker compose down -v
```

`docker compose down -v` removes the local MySQL, Redis, and Kafka volumes.

## Business Features

### Internal Exchange Core

- Place orders, amend resting orders, cancel-replace orders, bulk-cancel open orders, and query open or historical orders.
- Matching engine support for LIMIT / MARKET, GTC / IOC / FOK, partial fills, price-time priority, and self-match prevention.
- Order book depth, trade tape, ticker, kline, and depth delta.
- Margin deposit/withdrawal state machine, cross/isolated margin transfer, account query, risk snapshot, transfer query, and ledger query.
- Position updates, maker/taker fees, referral rebates, and realized PnL.
- Funding settlement, liquidation, insurance fund, and ADL queue MVP.
- Snapshot, event replay, recovery, and validation entry points.
- Domain event publishing, outbox retry, DLQ, and Kafka event store.
- Lightweight operations metrics endpoint for order status counts, order latency, cancel counts, and trade event counts.
- SSE / WebSocket push for market events and private user events, including opt-in cancel-on-disconnect for user WebSocket sessions.

### Polymarket Integration

- Gamma market discovery and sync progress tracking.
- Market metadata persistence and price refresh.
- CLOB API key create / derive.
- Session Signer init / confirm / list / revoke.
- CLOB order EIP-712 signing and L2 HMAC request signing.
- Real Polymarket order placement, local order query, single-order sync, cancel, and reconcile.
- ERC20 collateral allowance and ERC1155 conditional-token approval status checks.
- Polymarket user WebSocket subscription for order / trade / settlement lifecycle events, published to Kafka topic `polymarket.user.events`.

## Business Modules

| Module | Main package | Responsibility |
| --- | --- | --- |
| API layer | `interfaces.web` | REST controllers, DTOs, validators, exception handler, SSE/WebSocket config |
| Event consumers | `interfaces.consumer` | Consume Kafka topics and connect events to push, tracking, or follow-up business handling |
| Use cases | `application.usecase` | Accept API intent and coordinate one business action |
| Application services | `application.service` | Orchestrate repositories, domain services, event publishers, and infrastructure |
| Schedulers | `application.scheduler` | Periodically trigger snapshots, funding, reconciliation, and Polymarket sync |
| Domain model | `domain.model` | Orders, accounts, positions, ledger entries, Polymarket markets/orders/sessions |
| Domain services | `domain.service` | Matching rules, order book, Polymarket market/order/session/user-WS logic |
| Repository contracts | `domain.repository` | Domain data-access contracts and JPA repositories |
| Infrastructure | `infra` | Kafka, Redis, matching engine, Web3j, OkHttp, Spring config |
| Resources | `src/main/resources` | Profiles, Flyway migration, static test page |

## Architecture

The project uses a layered architecture. Dependencies point inward: external adapters depend on application/domain layers, while the domain layer does not depend on Spring Web or concrete infrastructure.

```text
Client / Script / Frontend
        |
        v
interfaces.web Controller / DTO / Validator
        |
        v
application UseCase / Service / Scheduler
        |
        v
domain Model / Service / Event / Repository Contract
        |
        v
infra Redis / Kafka / MySQL JPA / Matching / HTTP / Web3j
```

Trading event flow:

```text
POST /api/order/place
  -> PlaceOrderUseCase
  -> RiskService pre-check
  -> OrderService
  -> InMemoryMatchingEngine
  -> Position / Account / Ledger update
  -> EventStore + DomainEventPublisher
  -> Kafka topics
  -> Consumer / PushGateway / Recovery
```

## Order Placement Flow

The internal exchange order entry point is `POST /api/order/place`. The current implementation synchronously performs basic validation, risk pre-check, matching, accounting updates, event publication, and then returns `accepted`. This is still MVP behavior. See [todo.md](todo.md) for the remaining production work around durable matching, broader replay, and stronger accounting consistency.

```text
HTTP POST /api/order/place
  |
  v
OrderController.place()
  - Receives PlaceOrderRequest
  - Runs Bean Validation for uid / symbol / side / type / qty / leverage / marginMode
  - Converts the request into PlaceOrderCommand
  |
  v
PlaceOrderUseCase.handle()
  - Validates command basics
  - Loads SymbolConfig and resolves Symbol / TimeInForce / MarginMode
  - Requires price for LIMIT orders; keeps price null for MARKET orders
  - Builds the Order aggregate
  |
  v
RiskService.preCheckAndReserve()
  - Checks symbol config, leverage, price deviation, reduceOnly, and available balance
  - Calculates required order reserve
  - Writes order-reserve ledger entries
  |
  v
OrderService.processOrder()
  - Submits the Order to MatchingEngine
  - Gets MatchingResult: trades + affectedOrders
  - Appends each TradeExecuted to EventStore and receives seq
  - Uses idempotency key to prevent duplicate trade accounting
  - Updates Position, position margin, realized PnL, fee, and rebate
  - Reconciles reserve for all affectedOrders
  - Persists order states through OrderRepository
  - Updates MarketDataService trade tape / ticker / kline / depth delta
  - Publishes trade events to Kafka through DomainEventPublisher
  |
  v
Kafka / Consumer / Push
  - trade.executed: trade events
  - order.lifecycle: order lifecycle audit events
  - event.store.trade: replayable events
  - TradeEventConsumer pushes market/user events
```

A successful order request does not always mean the order is filled:

- If a LIMIT order is not fully filled and is eligible for GTC posting, the remainder stays in the order book.
- MARKET, IOC, FOK, and POST_ONLY conditions can cause partial fill, expiry, or rejection.
- If risk or accounting pre-check fails, the order is rejected before entering the matching engine.
- The API currently returns `accepted`; use `GET /api/order/open`, `GET /api/order/all`, or the event stream to observe the final state.

Order management endpoints:

- `PATCH /api/order/{orderId}` amends a resting LIMIT order without taking liquidity. The request can change `price`, remaining `qty`, and `clientOrderId`; reserve is reconciled to the new remaining order.
- `POST /api/order/{orderId}/replace` cancels the original open order first, then submits a replacement order with the provided `price`, `qty`, or `clientOrderId`.
- `DELETE /api/order/open?uid=...&symbol=...` bulk-cancels open orders and releases remaining order reserve. Omitting `symbol` cancels all open orders for the user.
- `GET /api/order/{orderId}/lifecycle` reads the durable order lifecycle event log; `GET /api/order/{orderId}/projection` reads the latest-state projection; `POST /api/order/{orderId}/projection/rebuild` rebuilds that projection from the event log.
- `/ws/user/{uid}?cancelOnDisconnect=true&symbol=BTCUSDT` enables opt-in cancel-on-disconnect for that user WebSocket connection. Omitting `symbol` cancels all open orders for the user when the connection closes. A reconnecting client can pass `resumeConnectionId=<oldSessionId>` to move the cancel-on-disconnect registration to the new WebSocket session before the old close event is processed.
- `GET /api/depth/{symbol}` returns full book levels with `version` and CRC32 `checksum`. `GET /api/market-data/{symbol}/depth-delta` returns the same monotonic depth `version` and checksum for client-side snapshot + delta validation.

Polymarket data flow:

```text
Gamma API / CLOB API / User WebSocket
  -> Polymarket domain service
  -> JPA entity / Redis state
  -> Kafka polymarket.user.events
  -> PolymarketUserEventConsumer
  -> Local order tracking / reconcile
```

Directory layout:

```text
src/main/java/com/example/exchange
├── interfaces
│   ├── web          # REST / DTO / Validator / Exception / Push config
│   └── consumer     # Kafka consumer
├── application
│   ├── command      # UseCase command
│   ├── usecase      # Application flow entry points
│   ├── service      # Application services
│   ├── scheduler    # Scheduled jobs
│   └── event        # Event publisher abstraction
├── domain
│   ├── model        # Entity / DTO / Enum
│   ├── service      # Matching, Polymarket, order-book domain services
│   ├── repository   # Repository contracts / JPA repositories
│   ├── event        # Domain events
│   └── util         # Signing, JSON, parser utilities
└── infra
    ├── config       # Spring/Kafka/Redis/Web3j/OkHttp config
    ├── kafka        # Kafka adapters
    ├── matching     # In-memory matching engine
    └── redis        # Redis repository implementations
```

## Main APIs

### Exchange

- `POST /api/margin/deposit`
- `POST /api/margin/withdraw`
- `POST /api/margin/transfer`
- `GET /api/margin/account?uid=1`
- `GET /api/margin/ledger?uid=1`
- `GET /api/margin/ledger/replay?uid=1&asset=USDT`
- `GET /api/margin/ledger/replay/compare?uid=1&asset=USDT`
- `GET /api/margin/transfers?uid=1`
- `GET /api/margin/risk?uid=1`
- `POST /api/margin/risk/snapshot?uid=1`
- `POST /api/margin/risk/snapshots`
- `GET /api/margin/risk/snapshot/latest?uid=1`
- `GET /api/margin/risk/snapshots?uid=1&limit=30`
- `POST /api/order/place`
- `PATCH /api/order/{orderId}`
- `POST /api/order/{orderId}/replace`
- `DELETE /api/order/{orderId}`
- `DELETE /api/order/open?uid=1&symbol=BTCUSDT`
- `GET /api/order/open?uid=1&symbol=BTCUSDT`
- `GET /api/order/all?uid=1&symbol=BTCUSDT`
- `GET /api/order/{orderId}/lifecycle`
- `GET /api/order/{orderId}/projection`
- `POST /api/order/{orderId}/projection/rebuild`
- `GET /api/order/projections?uid=1&symbol=BTCUSDT`
- `GET /api/depth/{symbol}?depth=10`
- `GET /api/market-data/{symbol}/ticker`
- `GET /api/market-data/{symbol}/trades`
- `GET /api/market-data/{symbol}/klines`
- `GET /api/market-data/{symbol}/depth-delta`
- `GET /api/market-data/{symbol}/stream`
- `GET /api/market-data/user/{uid}/stream`
- `PUT /api/risk/price-oracle`
- `GET /api/risk/price-oracle/{symbol}`
- `POST /api/risk/funding/settle`
- `GET /api/ops/metrics`
- `POST /api/risk/liquidate`
- `GET /api/risk/insurance-fund`
- `GET /api/recovery/reconcile/accounts`
- `POST /api/recovery/reconcile/accounts/report`
- `GET /api/recovery/reconcile/reports?limit=20`
- `GET /api/recovery/reconcile/reports/{reportId}`
- `GET /api/risk/adl-queue`
- `POST /api/recovery/recover/{uid}?fromSeq=0`
- `GET /api/recovery/validate/{uid}`
- `GET /api/recovery/outbox/dlq?limit=50`
- `POST /api/recovery/outbox/dead/{outboxId}/replay`
- `POST /api/recovery/outbox/dead/{outboxId}/compensate`

### Prediction / Polymarket

- `POST /api/prediction/markets/discover`
- `POST /api/prediction/markets/sync`
- `POST /api/prediction/markets/sync-reset`
- `GET /api/prediction/markets/sync-progress`
- `POST /api/prediction/markets/retry/{eventSlug}`
- `POST /api/prediction/markets/price-refresh`
- `GET /api/prediction/markets`
- `POST /api/prediction/clob/api-key/create?nonce=0`
- `GET /api/prediction/clob/api-key/derive?nonce=0`
- `POST /api/prediction/session/init`
- `POST /api/prediction/session/confirm`
- `GET /api/prediction/session/list?userAddress=0x...`
- `POST /api/prediction/session/revoke`
- `POST /api/prediction/session/revoke-all?userAddress=0x...`
- `POST /api/prediction/orders`
- `GET /api/prediction/orders/local`
- `GET /api/prediction/orders/local/{internalOrderId}`
- `POST /api/prediction/orders/local/{internalOrderId}/sync`
- `POST /api/prediction/orders/local/{internalOrderId}/cancel?commandId=...`
- `POST /api/prediction/orders/reconcile`
- `GET /api/prediction/orders/trades`
- `POST /api/prediction/ws/user/start`
- `POST /api/prediction/ws/user/stop`
- `GET /api/prediction/ws/user/status`
- `GET /api/prediction/approve/collateral/allowance`
- `GET /api/prediction/approve/conditional-tokens/status`

## Kafka Topics

`docker-compose.yml` creates the topics currently used by the application:

- `trade.executed`
- `order.lifecycle`
- `event.store.trade`
- `funding.settled`
- `position.liquidated`
- `domain.events`
- `polymarket.user.events`

## Configuration

Main configuration files:

- `src/main/resources/application.yml`: default profile and application name.
- `src/main/resources/application-dev.yml`: local MySQL, Redis, Kafka, Polymarket, and Web3 settings.
- `src/main/resources/application-prod.yml`: baseline production profile settings.
- `docker-compose.yml`: local Kafka, Redis, MySQL, and Kafka UI.

Polymarket `private-key`, CLOB `api-key/api-secret/api-passphrase`, and relayer keys must not be committed to Git. Production should inject them through environment variables or a secret manager.

Secret-related environment variables:

- `POLYMARKET_WALLET_PRIVATE_KEY`
- `POLYMARKET_WALLET_FUNDER_ADDRESS`
- `POLYMARKET_WALLET_SIGNATURE_TYPE`
- `POLYMARKET_CLOB_API_KEY`
- `POLYMARKET_CLOB_API_SECRET`
- `POLYMARKET_CLOB_API_PASSPHRASE`
- `POLYMARKET_RELAYER_API_KEY`
- `WEB3_POLYGON_RPC_URL`

Protected trading, funds, and admin APIs are covered by the `security-controls` interceptor. It applies rate limiting, optional IP allowlist checks, and `SECURITY_AUDIT` logs.

Security control environment variables:

- `SECURITY_CONTROLS_ENABLED`
- `SECURITY_CONTROLS_AUDIT_ENABLED`
- `SECURITY_CONTROLS_RATE_LIMIT_ENABLED`
- `SECURITY_CONTROLS_REQUESTS_PER_MINUTE`
- `SECURITY_CONTROLS_MAX_TRACKED_KEYS`
- `SECURITY_CONTROLS_IP_ALLOWLIST_ENABLED`
- `SECURITY_CONTROLS_IP_ALLOWLIST`
- `SECURITY_CONTROLS_CLIENT_IP_HEADER`

Protected APIs also support `api-auth` authentication and authorization. Production enables it by default and accepts either SHA-256 hashed API keys or HS256 JWT bearer tokens. Admin APIs require `ROLE_ADMIN` or `admin` scope; trading APIs require trader/user roles or trading scopes; funds APIs require funds/admin roles or funds scopes.

API auth environment variables:

- `API_AUTH_ENABLED`
- `API_AUTH_API_KEY_ENABLED`
- `API_AUTH_API_KEY_HEADER`
- `API_AUTH_API_KEYS`
- `API_AUTH_JWT_ENABLED`
- `API_AUTH_JWT_HMAC_SECRET`
- `API_AUTH_CLOCK_SKEW_SECONDS`

`API_AUTH_API_KEYS` format:

```text
keyId:sha256Hex:ROLE_ADMIN|ROLE_TRADER:admin|trade:write;trader:sha256Hex:ROLE_TRADER:trade:write
```

## Tests

```bash
./mvnw test
```

Current test coverage includes:

- In-memory matching engine FIFO, post-only, and self-match prevention.
- Position, fee, ledger, market-data, and event publishing after fills.
- Mark/index price oracle, risk tiers, funding settlement, liquidation, insurance fund, and reconciliation.

## Related Documents

- [todo.md](todo.md)
- [繁體中文 README](../zh-TW/README.md)
- [繁體中文 TODO](../zh-TW/todo.md)
- [matching README](../../src/main/java/com/example/exchange/infra/matching/README.md)
- [API curl scripts](../../shells/api-curls/README.md)
