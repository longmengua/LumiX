# Task: Custodial Polymarket Routing And Key Security

Status: `todo`
Size: L
Token Budget: 60k-120k
Timebox: 1 day

## Goal

Route Polymarket-backed orders through the exchange backend without exposing EOA wallets or Polymarket credentials to end users.

## Scope

- In scope:
  - Secret-reference configuration for Polymarket signer and CLOB L2 credentials.
  - `PolymarketSigningService` boundary with policy checks and audit trail.
  - Async venue-routing command from internal order acceptance to Polymarket CLOB placement.
  - Kill switch, per-market enablement, and notional caps.
  - Reconciliation between external Polymarket state and internal order/ledger state.
- Out of scope:
  - Building a production HSM/MPC provider integration in the first slice.
  - Moving real funds before signing policy, audit, and reconciliation are in place.
  - Exposing private keys or CLOB credentials to frontend clients.

## First Implementation Slice

1. Add config records for `signer-ref`, `credential-ref`, `funder-address`, `signature-type`, and routing enablement.
2. Add startup validation that rejects raw key material in production profile.
3. Introduce `PolymarketSigningService` and a local dev adapter that signs only when policy allows.
4. Persist signed-order audit records with trace ID, actor, market, tokenId, notional, policy result, and idempotency key.
5. Move direct Polymarket order placement behind a routing command/service boundary.

## Acceptance Criteria

- Frontend order entry never receives private key, CLOB secret, or passphrase values.
- Production startup fails if raw Polymarket key material is configured directly.
- Polymarket order signing goes through `PolymarketSigningService`.
- Signing policy rejects unknown market/token, disabled market, missing idempotency key, and notional-cap breach.
- Focused tests cover config validation, signing policy allow/reject paths, and idempotent routing command replay.
- Docs and AI maps are updated with the new ownership boundary.

## Read First

- [../../en/custodial-polymarket-routing-security.md](../../en/custodial-polymarket-routing-security.md)
- [../../zh-TW/custodial-polymarket-routing-security.md](../../zh-TW/custodial-polymarket-routing-security.md)
- [../../ai/maps/polymarket-security.md](../../ai/maps/polymarket-security.md)
- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
