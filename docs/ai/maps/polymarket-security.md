# Polymarket And Security Map

## Polymarket Market And Order Flow

- Controller: `interfaces.web.controller.PredictionOrderController`
- Market services:
  - `PolymarketDiscoveryService`
  - `PolymarketMarketService`
  - `PolymarketMetadataSyncService`
  - `PolymarketSyncService`
  - `PolymarketPriceService`
- Order services:
  - `PolymarketOrderService`
  - `PolymarketOrderTrackingService`
  - `PolymarketClobTradingClient`
- Session services:
  - `PolymarketSessionService`
  - `PolymarketClobAuthService`
- User events:
  - `PolymarketUserWebSocketService`
  - `PolymarketUserEventService`
  - `interfaces.consumer.PolymarketUserEventConsumer`
- JPA repositories: `domain.repository.jpa.Prediction*Repository`
- Config: `infra.config.PolymarketConfigs`

Implemented baselines:
- Fuller CLOB trade/settlement state machine; `PolymarketOrderStateMachine` exposes the local/CLOB/trade/settlement transition matrix, prevents stale active CLOB payloads from downgrading local filled/settled terminal orders or matched size, promotes user-channel trade matches into the local matched lifecycle, place has a `clientRequestId` local idempotency baseline, cancel can use durable `commandId` records, cancel replays already-recorded cancel/uncertain statuses locally, and reconcile can resolve uncertain cancel from remote CLOB status while sync/reconcile skip unchanged local writes.
- `PolymarketUserEventService` persists user-channel events by `eventKey`, no-ops duplicate replays, treats unique-key save races as duplicate replay before applying order side effects, routes order/trade/settlement status updates through `PolymarketOrderStateMachine`, resolves payload-only order/trade ids, and persists trade matches into the local `PredictionPolymarketOrder` lifecycle projection.
- Approval reads already have TTL cache and owner-scoped clear.
- `RpcTransactionTrackingService` persists backend-observed RPC transaction command/chain/wallet/fingerprint/txHash/status records, rejects command conflicts, and exposes unresolved outcome reporting.
- `PolymarketResponseSchemaValidator` produces versioned Gamma `/events` and `/markets` schema reports plus CLOB order-operation schema reports; Gamma DTOs ignore unknown remote fields while the reports keep remote-field drift visible in logs/tests.

Remaining production TODO:
- Settlement terminal-state downgrade tests.
- Independent user WebSocket worker with checkpoint, dedup, persistence, replay.

## Signing And External APIs

- EIP-712 and CLOB signing: `domain.util.PolymarketEip712Signer`, `PolymarketClobOrderSigner`
- RPC transaction tracking: `application.service.RpcTransactionTrackingService`
- External API config: `infra.config.OkHttpConfig`
- Gamma client contract: `domain.repository.client.PredictionGammaMarketClient`
- External API idempotency inventory: `docs/en/external-api-idempotency.md`

Secrets must come from env/secret manager:
- `POLYMARKET_WALLET_PRIVATE_KEY`
- `POLYMARKET_CLOB_API_KEY`
- `POLYMARKET_CLOB_API_SECRET`
- `POLYMARKET_CLOB_API_PASSPHRASE`
- `POLYMARKET_RELAYER_API_KEY`
- `WEB3_POLYGON_RPC_URL`

## Security And Tracing

- Interceptors:
  - `RequestLoggingInterceptor`
  - `ApiAuthenticationInterceptor`
  - `ProtectedApiSecurityInterceptor`
- Auth helpers:
  - `ApiKeyAuthenticator`
  - `JwtAuthenticator`
  - `IpAllowlist`
  - `ProtectedApiClassifier`
- Trace helper: `infra.tracing.TraceContext`
- Tests:
  - `RequestLoggingInterceptorTest`
  - `ApiAuthenticationInterceptorTest`
  - `ProtectedApiSecurityInterceptorTest`
  - `ApiKeyAuthenticatorTest`
  - `JwtAuthenticatorTest`
  - `IpAllowlistTest`

Session signer lifecycle baseline:
- `PolymarketSessionService` rejects inactive or expired session use before limit consumption, marks expired stale records `EXPIRED`, revokes both `PENDING` and `ACTIVE` sessions on wallet-wide revoke, and logs abnormal limit-breach / invalid-use warnings for audit review.

Remaining production TODO:
- Distributed tracing export, dashboards, sampling policy.
