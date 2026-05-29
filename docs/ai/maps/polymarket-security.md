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

Remaining production TODO:
- Local/CLOB/trade/settlement order state machine.
- Versioned Gamma/CLOB response schemas.
- Idempotent sync/reconcile commands and fuller cancel uncertainty handling; place has a `clientRequestId` local idempotency baseline, and cancel replays already-recorded cancel statuses locally.
- Independent user WebSocket worker with checkpoint, dedup, persistence, replay.
- Allowance/approval cache and expiry policy.

## Signing And External APIs

- EIP-712 and CLOB signing: `domain.util.PolymarketEip712Signer`, `PolymarketClobOrderSigner`
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

Remaining production TODO:
- Session signer expiration, revocation, audit, abnormal-use detection.
- Distributed tracing export, dashboards, sampling policy.
