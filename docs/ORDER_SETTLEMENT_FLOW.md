# Order Settlement Flow

This document describes the required production spot order flow for LumiX.

It defines the real end-state architecture and is not a claim that the current repo already implements it.

## Preconditions

- user authentication is valid
- symbol metadata is active and approved
- trading pair precision, limits, and fee rules are configured
- risk engine is available
- ledger and reservation services are available
- matching-core integration is available

## 1. User Submits Order

- User sends order request with `request_id` and, where applicable, a client order identifier.
- API layer authenticates the request and enforces idempotency rules.
- Request is normalized into the Java spot-order domain model.

## 2. Validate Symbol / Precision / Risk

Order service validates:

- symbol exists and is tradable
- side and order type are supported
- quantity precision and price precision
- min/max quantity and notional
- account status and market status
- pre-trade risk rules such as size limits, reduce-only gates, or market pauses

If validation fails, the order is rejected before any funds are reserved.

## 3. Calculate Required Funds

The required-funds calculation depends on side and type.

- Limit buy:
  - reserve quote notional based on order price and quantity
  - include defined fee policy treatment or fee buffer
- Market buy:
  - reserve by approved market-buy policy, such as quote amount or guarded notional estimate
- Limit sell:
  - reserve base quantity being sold
- Market sell:
  - reserve the sell quantity in base asset

The required-funds formula must be deterministic and versioned.

## 4. Reserve Funds

- Spot order service calls the reservation engine.
- Reservation engine verifies `available >= required_amount`.
- Funds move from available to locked through the ledger-backed balance model.
- Reservation result is stored with a durable `reservation_id`.

If reservation fails, the order is rejected and never reaches persistence or matching.

## 5. Persist Order

- Order is written to durable storage before external submission.
- Persisted state includes:
  - `order_id`
  - `request_id`
  - `reservation_id`
  - normalized order fields
  - initial status
  - audit timestamps

If persistence fails after reservation succeeds, the reservation must be rolled back safely.

## 6. Submit To Matching Core

- Java order service submits the persisted order through `MatchingEngineClient`.
- The C++ matching core becomes the authority for order-book placement and execution decisions.
- Order service records whether the order is awaiting core acknowledgement, accepted, or rejected.

Java must not fill or cross the order locally.

## 7. Receive Fill Events

- Matching core emits authoritative order and fill events with event IDs and sequence numbers.
- Java ingests these events idempotently.
- Order service updates working, partially filled, cancelled, rejected, or filled state from authoritative events only.

## 8. Calculate Fees

For each fill event:

- determine maker/taker side if applicable
- determine buyer and seller settlement legs
- calculate fees using the active fee schedule
- produce a deterministic settlement payload

Fee calculation must be versioned and auditable.

## 9. Commit Ledger Entries

Settlement engine consumes the fill event and:

- commits the appropriate amount from the reservation
- posts double-entry journals for asset transfer and fee collection
- credits the counter-asset to the correct account
- records settlement status and business references

Duplicate fill events must not double-settle.

## 10. Release Unused Funds

Unused locked funds are released when the order reaches a terminal condition.

Examples:

- unfilled cancel releases the full reservation
- partially filled cancel releases only the remaining lock
- completed buy releases unused quote-side reserve after final fill and fee outcome are known
- rejected order releases or rolls back any pre-submission reservation

## 11. Update Order Status

Order state moves based on authoritative core and settlement signals, not UI assumptions.

Typical states:

- `REJECTED`
- `WORKING`
- `PARTIALLY_FILLED`
- `FILLED`
- `CANCELLED`
- `SETTLEMENT_PENDING`
- `SETTLED`

Terminal order status and terminal settlement status must both be queryable.

## 12. Publish Market Data

After authoritative matching events arrive:

- order-book depth updates are published
- recent trades are published
- ticker and kline derivatives update

Market data must be derived from authoritative event streams, not from order-service guesses.

## 13. Reconcile

Reconciliation must verify:

- persisted order state versus matching-core event state
- reservation remaining amount versus order remaining quantity
- fill events versus settlement journals
- settled balances versus balance projection
- public market-data projections versus authoritative event stream

Any mismatch opens a case. It does not permit a silent fix.

## Failure Branches

### Reservation succeeds but persistence fails

- rollback the reservation
- mark request as failed with durable audit references

### Persistence succeeds but submission acknowledgement is uncertain

- keep order in a protected pending-core state
- do not release the reservation until authoritative status is known

### Fill arrives but settlement fails

- keep settlement in retry or incident state
- do not double-apply commit/release during retries
- escalate to reconciliation and compensation workflow if needed

### Cancel requested while fills may still be in flight

- rely on authoritative core state
- release only the known remaining reservation
- reconcile against any late fill events

## Phase 11 Conclusion

LumiX cannot claim a production spot order service until the full sequence below exists together:

1. validate order
2. reserve funds
3. persist order
4. submit to C++ matching core
5. receive authoritative fill events
6. calculate fees
7. commit ledger entries
8. release unused funds
9. update order status
10. publish market data
11. reconcile outcomes
