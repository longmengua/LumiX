# Phase 16 - Production Spot Order Service

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Replace the validation-only spot stub with a production Java spot order orchestration service that validates, reserves, persists, submits, cancels, and exposes order state without fake matching.

## Why this phase exists

User-facing spot trading cannot exist without a durable order lifecycle, fund reservation, idempotency, and a strict integration boundary to the matching core.

## Dependencies

- Previous phases required: Phase 12, Phase 13, Phase 14, Phase 15
- External dependencies if any: symbol metadata source, risk rules, matching boundary contract
- Blocking risks: fake matching in Java, broken idempotency, incorrect order status transitions

## Scope

- Validate order
- Calculate required funds
- Reserve funds
- Persist order
- Submit to matching boundary
- Cancel order
- Order status lifecycle
- Client order id idempotency
- Order query APIs
- No fake matching

## Non-goals

- Matching engine implementation
- Fill generation in Java
- Settlement runtime
- Market-data runtime

## Required deliverables

- Production `SpotOrderService`
- Order persistence model and repository layer
- Submission and cancel orchestration through `MatchingEngineClient`
- Order status lifecycle rules
- Client-order-id idempotency behavior
- Order query APIs
- Spot-order test suite

## Acceptance criteria

- Orders are rejected before reservation if validation fails
- Orders cannot be submitted before reservation and persistence succeed
- Cancel does not release funds without authoritative remaining state
- Client order id idempotency is enforced
- No Java code path performs matching locally

## Required tests

- Validation tests
- Required-funds calculation tests
- Reservation-before-submit tests
- Persistence failure handling tests
- Client-order-id idempotency tests
- Cancel lifecycle tests
- Query API tests

## Files / modules likely affected

- `server/src/main/java/com/lumix/spot/`
- API/controller packages
- repositories for spot orders

## Data model impact

- Uses orders and client-id uniqueness structures from Phase 12
- May require order-event or status-history metadata

## API impact

- Introduces production spot order submit/cancel/query API surfaces
- Still depends on later phases before a full trading claim

## Security impact

- Must enforce authenticated ownership of order actions
- Must audit sensitive order submissions and cancels

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because order orchestration directly controls reservation and downstream settlement exposure

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: incorrect orchestration can lock, misroute, or expose user funds

## Cannot claim yet

- matching completed
- settlement completed
- production market data completed
- production trading completed

## Next phase handoff

Phase 17 implements the deterministic C++ matching core that this phase must submit to.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/TRADING_CORE_BOUNDARIES.md, docs/FUNDS_SAFETY_MODEL.md, docs/ORDER_SETTLEMENT_FLOW.md, and docs/phases/PHASE_16_PRODUCTION_SPOT_ORDER.md.

Goal: implement Phase 16 only - Production Spot Order Service.
Scope: order validation, required-funds calculation, reservation, persistence, submit/cancel through MatchingEngineClient, order lifecycle, client-order-id idempotency, and query APIs.
Non-goals: matching engine, fake Java matching, settlement runtime, market-data runtime, later phases.
Deliverables: production spot order orchestration, tests, and progress/doc updates tied to real code.
Tests: validation, funds calculation, reservation-before-submit, persistence failure, client-order-id idempotency, cancel lifecycle, query APIs, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 16 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: matching completed, settlement completed, production market data completed, production trading completed.
Final output format: Changed Files, Summary, What Phase 16 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
