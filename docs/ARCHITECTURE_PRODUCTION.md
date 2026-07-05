# Production Exchange Architecture

This document defines the production architecture boundary model introduced in Phase 11.

It is a design and ownership document only. It does not claim that the implementation already exists.

## Domain Boundaries

| Domain | Owns | Must not do |
| --- | --- | --- |
| API and Auth | request authentication, authorization, idempotency headers, rate limiting, request shaping | mutate balances, simulate matching, bypass risk |
| Account Query | read models for balances, orders, positions, wallet history | perform writes or implicit balance mutation |
| Ledger Engine | immutable double-entry journals and balance-affecting state transitions | match orders, broadcast chain tx, generate market data |
| Reservation Engine | available-to-locked state transitions and reservation lifecycle | settle fills without ledger, create trades, invent balances |
| Spot Order Service (Java) | validate requests, calculate required funds, reserve funds, persist orders, submit or cancel via integration boundary | match orders locally, fabricate fills, publish authoritative market data |
| Matching Core (C++) | authoritative order book, sequencing, order acceptance or rejection, fill generation | write balances, write ledger journals, handle wallets, do admin adjustments |
| Settlement Engine | consume fill events, calculate fees, commit ledger entries, release unused reservations | generate fills, own order book, bypass reservation invariants |
| Market Data Pipeline | derive depth, trades, ticker, and kline from authoritative events | synthesize fake execution state, mutate balances |
| Wallet Deposit System | observe chains, manage address assignment, validate confirmations, hand confirmed credits to ledger | credit balances without ledger, settle orders |
| Wallet Withdrawal System | enforce secure withdrawal flow, reserve funds, broadcast chain tx after approvals, finalize commit or rollback | bypass risk, bypass admin approval, mutate balances outside ledger |
| Treasury | hot/cold wallet inventory, sweep and refill policy, custody operations | act as a user-facing trading service |
| Risk Engine | pre-trade, account, withdrawal, and market guardrails; kill switches | settle trades, mutate balances directly |
| Admin Back Office | RBAC, maker-checker approvals, operations, incident tools, audits | silently mutate user funds outside journaled workflows |
| Reconciliation | compare internal and external state, open cases, drive compensation workflow | apply silent fixes without review |

## Trading Flow

1. User submits an order through authenticated API or UI.
2. Java order service validates symbol, precision, limits, and risk prerequisites.
3. Order service calculates required funds and requests a reservation.
4. Reservation engine moves funds from available to locked through ledger-backed state transitions.
5. Order service persists the order before external submission.
6. Order service submits the order to the C++ matching core through `MatchingEngineClient`.
7. Matching core becomes the authority for order-book placement and fill generation.
8. Matching core emits authoritative order and fill events with sequencing and replay support.
9. Settlement engine consumes fill events and converts them into journaled asset movement plus reservation commits/releases.
10. Order service updates durable order state from authoritative events.
11. Market data pipeline derives depth, recent trades, ticker, and candles from the same event stream.
12. Reconciliation verifies that persisted orders, matching events, settlement state, and market-data projections remain aligned.

## Asset Flow

- Deposit asset flow:
  - address assignment
  - chain observation
  - confirmation policy
  - risk or compliance checks
  - ledger credit
  - balance projection update
  - user available balance
- Withdrawal asset flow:
  - user request
  - security verification
  - risk checks
  - fund reservation
  - admin review if required
  - chain broadcast
  - on-chain finality tracking
  - ledger commit or rollback
  - treasury and chain reconciliation
- Trading asset flow:
  - reserve
  - fill-driven commit
  - fee posting
  - release of unused lock
  - balance projection update

## Order Lifecycle

Authoritative order lifecycle for spot:

1. `RECEIVED`
2. `VALIDATED`
3. `RESERVATION_PENDING`
4. `RESERVED`
5. `PERSISTED`
6. `SUBMITTING_TO_CORE`
7. `WORKING`
8. `PARTIALLY_FILLED`
9. `FILLED` or `CANCEL_PENDING`
10. `CANCELLED` or `REJECTED`
11. `SETTLEMENT_PENDING`
12. `SETTLED`

Rules:

- Java owns user-facing order persistence and query state.
- C++ owns the authoritative order-book and execution state.
- Settlement owns fill application status.
- No service may mark an order filled without an authoritative fill event from the matching core.

## Ledger Lifecycle

1. Business service prepares a journal or a reservation-related mutation request.
2. Ledger validates account existence, asset, direction, amount, business reference, and idempotency reference.
3. Ledger verifies that the journal balances and that the preconditions are satisfied.
4. Ledger persists immutable journal records.
5. Ledger triggers balance projection updates or equivalent state changes.
6. Ledger emits audit and reconciliation metadata.
7. Ledger mutation becomes the only authoritative source for downstream balance state.

## Reservation Lifecycle

1. Reservation request created with `reservation_id`, `request_id`, user, account, asset, and reason.
2. Precondition check confirms `available >= requested_amount`.
3. Reservation becomes `ACTIVE` after available is reduced and locked is increased.
4. Partial fill causes partial `COMMIT` against the reservation.
5. Remaining reservation stays active until fully consumed, released, or cancelled.
6. Cancel or expiry triggers `RELEASE`.
7. Failed downstream flow may trigger `ROLLBACK` if no irreversible external action happened.

## Settlement Lifecycle

1. Settlement receives an authoritative fill event.
2. Event is deduplicated by event ID and sequence.
3. Fees, buyer/seller movements, and reservation usage are calculated.
4. Ledger journals are created and validated.
5. Reservation commit and release are applied idempotently.
6. Settlement status is recorded as success, retryable failure, or terminal exception.
7. Downstream events feed order state, account history, and reconciliation.

## Deposit Lifecycle

1. User obtains a deposit address from the wallet domain.
2. Chain watcher or provider callback detects an inbound transaction.
3. Deposit record enters pending or confirming status.
4. Confirmation policy and risk/compliance checks run.
5. Confirmed deposit is credited through the ledger.
6. Balance projection updates user funds.
7. Reconciliation matches internal credit records against chain activity.

## Withdrawal Lifecycle

1. User submits a withdrawal request.
2. Security checks validate session, 2FA, whitelist, and policy requirements.
3. Risk engine evaluates limits, patterns, and account state.
4. Reservation engine locks the requested amount plus any required fee handling.
5. Admin review runs when required by policy.
6. Wallet gateway broadcasts the chain transaction.
7. Chain finality updates the withdrawal state.
8. Ledger commits the reserved funds or rolls them back on failure.
9. Treasury and chain reconciliation confirm final state.

## Reconciliation Lifecycle

1. Internal state is compared across ledger, balance projection, reservations, orders, fills, settlements, and wallet records.
2. External state is compared against chain data, treasury balances, and provider records.
3. Any mismatch opens a case rather than applying a silent fix.
4. Replay tools reconstruct the disputed state.
5. Compensation, if needed, goes through a journaled and approved workflow.
6. Case closure requires an audit trail and residual-risk note.

## Risk Control Points

- API authentication and request idempotency
- Pre-trade symbol, precision, notional, and size checks
- Pre-reservation available-balance checks
- Pre-submit order throttles and risk limits
- Post-fill settlement anomaly checks
- Deposit confirmation and compliance screening
- Withdrawal security, risk review, and kill switch checks
- Market pause, reduce-only, and emergency stop controls
- Reconciliation mismatch escalation

## Admin Control Points

- User freeze and account restriction
- Symbol pause and reduce-only mode
- Withdrawal pause and API withdraw gating
- Manual review queues for sensitive wallet flows
- Compensation approvals and dual control
- Replay, audit, and incident review tools

Admin operations must never bypass:

- ledger journaling
- reservation invariants
- audit logging
- maker-checker controls for high-risk actions

## Audit Requirements

- Every high-risk request must carry a durable business reference such as `request_id`, `reservation_id`, `event_id`, or `tx_hash`.
- Matching events must carry monotonic sequence metadata.
- Every ledger mutation must be journaled immutably.
- Every admin action must record actor, reason, before-state, after-state, and approval chain.
- No silent data repair is allowed.
- Replay and reconciliation data must be retained long enough to investigate disputes.

## Data Ownership Rules

- Matching core owns authoritative order-book transitions and fill generation.
- Java order service owns user-facing order persistence and request orchestration.
- Ledger owns balance-affecting mutation history.
- Reservation owns locked-funds lifecycle.
- Settlement owns fill-application progress.
- Wallet owns chain-observation and broadcast state, but not the authoritative account balance.
- Market data owns derived public views only.
- Reconciliation owns mismatch detection and case state, not silent mutation rights.

No module may directly edit another module's authoritative state without going through its boundary contract.
