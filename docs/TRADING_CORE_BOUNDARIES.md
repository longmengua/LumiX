# Trading Core Boundaries

This document defines the non-negotiable production boundary between Java orchestration, the future C++ matching core, settlement, ledger, and market data.

## Non-Negotiable Statements

- Java backend cannot pretend to have completed matching.
- `MatchingEngineClient` is an integration boundary only.
- Production matching must be implemented as a deterministic C++ core.
- Java order services may orchestrate, validate, persist, and submit orders, but they may not become the authoritative order book.
- C++ matching core may generate order and fill events, but it may not mutate balances, wallets, or admin state.

## MatchingEngineClient Boundary

`MatchingEngineClient` exists to connect Java to the production matching core.

It is not:

- a fake matching engine
- a substitute for a C++ order book
- permission for Java to fabricate fills

The production implementation must support:

- order submission
- cancel submission
- authoritative acknowledgements
- authoritative rejects
- fill events
- deterministic sequence handling
- replay and gap detection

## Java Order Service Responsibilities

The Java order service is responsible for:

- request validation
- symbol and precision validation
- pre-trade risk checks
- fund calculation
- reservation requests
- durable order persistence
- idempotency handling
- submission and cancel orchestration through `MatchingEngineClient`
- order query APIs
- translating authoritative core events into user-facing order state

The Java order service is not responsible for:

- matching one user order against another
- maintaining the authoritative order book
- inventing fill prices or quantities
- final settlement without settlement-engine logic

## C++ Matching Core Responsibilities

The C++ matching core is responsible for:

- authoritative order-book state
- deterministic sequencing
- price-time priority
- order admission and rejection rules defined for the core
- order cancellation
- trade generation
- snapshots and replay
- emission of authoritative order and fill events

The C++ matching core must not:

- write user balances
- write ledger journals
- reserve or release funds
- perform wallet deposits or withdrawals
- perform admin adjustments

## Settlement Engine Responsibilities

The settlement engine is responsible for:

- consuming authoritative fill events
- calculating fees
- translating fills into journaled asset movement
- committing consumed reservations
- releasing unused reservations
- recording settlement success or failure state
- providing replay-safe, idempotent settlement behavior

The settlement engine is not responsible for:

- generating fills
- owning the order book
- broadcasting chain transactions

## Ledger Engine Responsibilities

The ledger engine is responsible for:

- immutable double-entry journals
- debit and credit validation
- balance-affecting mutation authority
- audit metadata
- projection inputs for balances

The ledger engine is not responsible for:

- order matching
- wallet provider communication
- public market-data generation

## Market Data Pipeline Responsibilities

The market-data pipeline is responsible for:

- deriving depth from authoritative order-book events
- deriving recent trades from fill events
- deriving ticker and kline from authoritative event history
- publishing snapshots and incremental updates

The market-data pipeline is not responsible for:

- simulating fills
- inventing order-book levels
- mutating orders or balances

## Forbidden Designs

The following designs are forbidden:

- Java `DefaultSpotOrderService` or any future Java service directly filling or crossing orders
- treating `MatchingEngineClient` as the matching engine itself
- publishing market data from UI mocks or manually edited tables
- letting the matching core write balances or journals
- settling trades without a journaled ledger mutation path
- releasing user funds before authoritative cancel or terminal order state is known

## Phase 11 Conclusion

Until the C++ matching core exists and `MatchingEngineClient` has a real production implementation, LumiX cannot claim:

- production matching
- production order book
- production fill handling
- production market data derived from matching events
