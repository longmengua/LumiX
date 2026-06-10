# Custodial Polymarket Routing And Key Security

This document defines the target architecture for offering Polymarket trading through this centralized exchange without requiring end users to connect an EOA wallet.

## Product Model

Users trade through the exchange account system. They authenticate to this platform, place orders through platform APIs, and see balances, orders, fills, and positions in the exchange UI.

Polymarket is treated as a backend execution venue. The platform owns wallet operations, CLOB authentication, order signing, routing, reconciliation, and risk controls.

Frontend clients must never receive:

- EOA private keys.
- Polymarket CLOB API secrets or passphrases.
- Long-lived privileged API keys.
- Raw signed Polymarket order payloads unless explicitly needed for read-only audit display.

## External Polymarket Model

Polymarket CLOB authentication uses:

- L1 authentication with a wallet private key for API credential creation/derivation and order payload signing.
- L2 authentication with CLOB API key, secret, and passphrase for authenticated CLOB requests.
- Deposit wallet / `POLY_1271` flow for new API users, where the funder is the deposit wallet address.

Reference: <https://docs.polymarket.com/api-reference/authentication>

## Target Flow

```text
User
  -> Exchange UI / API
  -> OrderController / prediction order entry
  -> Internal risk and balance freeze
  -> VenueRoutingService
  -> PolymarketSigningService
  -> PolymarketClobTradingClient
  -> Polymarket CLOB
  -> user channel / polling reconciliation
  -> internal order lifecycle, position, wallet ledger
```

## Key Classes

Use separate key classes with separate blast radiuses:

- Cold treasury key: offline or HSM/MPC-backed, used only for large treasury movement and signer funding.
- Hot execution signer key: online but tightly scoped to Polymarket order signing. It must have small venue exposure limits and fast rotation.
- Deposit wallet / funder address: Polymarket funder configured with `signatureType=3` / `POLY_1271`.
- CLOB L2 credentials: `apiKey`, `apiSecret`, `apiPassphrase` for authenticated CLOB requests.
- RPC provider key: Polygon RPC access only, not a trading signer.
- Internal auth keys: user JWT/API keys and service-to-service credentials.

## Secret Storage

Production must use a secret manager, HSM, KMS, or MPC service. Application config should contain secret references, not raw secrets.

Allowed production config shape:

```yaml
polymarket:
  wallet:
    signer-ref: polymarket/prod/hot-signer-v1
    funder-address: ${POLYMARKET_WALLET_FUNDER_ADDRESS}
    signature-type: 3
  clob:
    credential-ref: polymarket/prod/clob-l2-v1
```

Disallowed:

- Committing private keys or CLOB secrets to Git.
- Storing private keys in SQL plaintext.
- Returning CLOB secrets to frontend clients.
- Reusing dev/staging/prod credentials.

## Signing Service Boundary

Trading code must not read private keys directly. It should ask a dedicated signing service to sign a constrained payload:

```text
PolymarketSigningService.signOrder(unsignedOrder, policyContext)
```

The signing service must enforce:

- Chain ID is Polygon mainnet (`137`) unless explicitly configured for a non-production environment.
- Payload type is an allowed Polymarket order or CLOB auth payload.
- Token ID and market are enabled in local market config.
- Per-order, per-market, per-user, and daily venue notional limits.
- Idempotency key is present.
- Request has a trace ID, actor, and reason.
- Signing decision is written to append-only audit storage.

## Order Routing Rules

Internal order routing must be asynchronous and replayable:

1. Accept user order only after internal risk and balance checks pass.
2. Freeze internal funds or margin before external routing.
3. Write an internal order and routing command with idempotency key.
4. Worker picks routing command and creates unsigned Polymarket order.
5. Signing service signs the order if policy allows it.
6. CLOB client posts signed order with L2 headers.
7. Persist external order ID, remote status, and request/response fingerprint.
8. User-channel events or polling update local lifecycle.
9. Reconciliation compares local state, CLOB state, trades, and internal ledger.

No controller should synchronously hold a private key or block on a fragile external signing flow.

## Audit And Controls

Required controls before real funds:

- Operator kill switch for all Polymarket routing.
- Per-market disable switch.
- Hot signer notional caps.
- CLOB credential rotation runbook.
- Secret access audit.
- Signed-order audit trail.
- Reconciliation alert when remote fills and local ledger diverge.
- Geo/KYC/eligibility policy gate before users can route to Polymarket-backed markets.
- Emergency cancel-all procedure for Polymarket open orders.

## Local Development

Local development may use dummy keys only. If a real key or CLOB credential was ever stored in a local ignored file, rotate it before production use.

`application-dev.yml` and `application-prod.yml` must keep using environment variables or secret references for:

- `POLYMARKET_WALLET_PRIVATE_KEY`
- `POLYMARKET_WALLET_FUNDER_ADDRESS`
- `POLYMARKET_CLOB_API_KEY`
- `POLYMARKET_CLOB_API_SECRET`
- `POLYMARKET_CLOB_API_PASSPHRASE`
- `POLYMARKET_RELAYER_API_KEY`
- `WEB3_POLYGON_RPC_URL`

## First Implementation Slices

1. Introduce a secret-reference config model and reject production startup if raw key material appears in config.
2. Add `PolymarketSigningService` interface with a local dev implementation and a secret-manager/HSM adapter boundary.
3. Move Polymarket order placement behind an asynchronous venue-routing command.
4. Add signing policy checks and audit records.
5. Add operator kill switch and per-market routing controls.
6. Add reconciliation checks between external Polymarket order state and internal order/ledger state.
