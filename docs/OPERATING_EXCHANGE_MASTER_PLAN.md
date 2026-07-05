# Operating Exchange Master Plan

This is the single source of truth for LumiX phase planning, production readiness, and phase governance.

## Current Authoritative Status

- Phase 11 is completed as a documentation-only production architecture reset.
- Phase 12 through Phase 36 are planned and not started for runtime implementation.
- The next implementation phase is Phase 12 - Production Database Schema & Migration.
- Production trading is not completed.
- Production launch readiness is not completed.
- Matching engine, production order book, MatchingEngineClient real implementation, double-entry ledger engine, balance mutation, asset reservation / freeze engine, production spot order flow, settlement engine, real deposit crediting, real withdrawal finalization, production market data pipeline, admin operations backend, risk engine / kill switch, reconciliation / compensation engine, and production infra / CI-CD / launch readiness are not completed.

## Target Operating Exchange Scope

LumiX is being defined as a full operating exchange, not an MVP.

Required capability families:

- real user accounts and security
- production spot trading
- deterministic matching
- double-entry ledger
- fund reservation / freeze
- settlement
- deposit / withdrawal
- market data
- Open API
- admin back office
- risk controls
- reconciliation
- security and compliance
- observability
- deployment and launch readiness

## Phase Table

| Phase | Name | Status | Category | Dependency | Risk | Touches user funds | Human review required | Runtime completed | Short summary | Cannot claim yet |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 0 | Frontend bootstrap | completed | frontend foundation | none | Low | No | No | Yes | web scaffold and app shell | production exchange core |
| 1 | Layout / routes | completed | frontend foundation | Phase 0 | Low | No | No | Yes | routes, layout, shared UI | production exchange core |
| 2 | Auth / home | completed | frontend foundation | Phase 1 | Low | No | No | Yes | auth and market pages | production trading |
| 3 | Account center | completed | frontend foundation | Phase 2 | Low | No | No | Yes | account UI | production funds safety |
| 4 | Asset pages | completed | frontend foundation | Phase 3 | Low | No | No | Yes | asset and transfer UI | real wallet operations |
| 5 | Trading pages | completed | frontend foundation | Phase 4 | Low | No | No | Yes | spot/futures/margin UI | real matching |
| 6 | Orders / positions | completed | frontend foundation | Phase 5 | Low | No | No | Yes | order and position UI | real settlement |
| 7 | API / notifications | completed | frontend foundation | Phase 6 | Low | No | No | Yes | API key and notifications UI | production API security |
| 8 | Admin console UI | completed | frontend foundation | Phase 7 | Medium | No | No | Yes | admin UI | production admin ops |
| 9 | Server skeleton | completed | backend skeleton | Phase 8 | Medium | No | No | No | Spring Boot foundation and contracts | production runtime |
| 10 | Wallet / spot / API stubs | completed | backend stubs | Phase 9 | High | No | No | No | DTOs, interfaces, placeholders | production trading |
| 11 | Production architecture reset | completed_docs_only | architecture reset | Phase 10 audit | High | No | Yes | No | safety model and roadmap reset | production trading completed |
| 12 | Production DB schema & migration | planned / not started | funds safety / ledger foundation | Phase 11 | Critical | Yes | Yes | No | schema for accounts, journals, reservations, orders, fills, deposits, withdrawals, reconciliation, audit | ledger engine, balance mutation, freeze, matching, settlement, production trading |
| 13 | Double-entry ledger engine | planned / not started | funds safety / ledger foundation | Phase 12 | Critical | Yes | Yes | No | immutable double-entry posting and idempotency | order freeze, spot order flow, matching, settlement |
| 14 | Balance projection / reconciliation | planned / not started | funds safety / ledger foundation | Phase 13 | Critical | Yes | Yes | No | rebuildable projections and ledger-vs-balance checks | production reconciliation |
| 15 | Asset reservation / freeze engine | planned / not started | funds safety / ledger foundation | Phase 12-14 | Critical | Yes | Yes | No | reserve/release/commit/rollback | production fund freeze |
| 16 | Production spot order service | planned / not started | spot trading core | Phase 12-15 | Critical | Yes | Yes | No | validate, reserve, persist, submit, cancel, query | production spot order flow |
| 17 | C++ matching core | planned / not started | spot trading core | Phase 11 boundary | Critical | Yes | Yes | No | deterministic order book and matching | production matching / order book |
| 18 | Java ↔ C++ integration | planned / not started | spot trading core | Phase 16-17 | Critical | Yes | Yes | No | command/event protocol and replay safety | production matching integration |
| 19 | Trade settlement engine | planned / not started | spot trading core | Phase 13-18 | Critical | Yes | Yes | No | fill settlement and fee posting | settlement completed |
| 20 | Market data pipeline | planned / not started | spot trading core | Phase 17-19 | High | No | Yes | No | depth, tape, ticker, kline | production market data |
| 21 | Deposit system | planned / not started | wallet / treasury | Phase 12-14 | Critical | Yes | Yes | No | address generation and chain detection | real deposit |
| 22 | Withdrawal system | planned / not started | wallet / treasury | Phase 12-15, 21 | Critical | Yes | Yes | No | withdrawal approval and broadcast boundary | real withdrawal |
| 23 | Hot / cold wallet treasury | planned / not started | wallet / treasury | Phase 21-22 | Critical | Yes | Yes | No | treasury, sweeps, signer boundary | wallet safety completed |
| 24 | Production Open API | planned / not started | API / admin / risk / liquidity | Phase 16, 20 | High | Yes | Yes | No | API key, signature, rate limit | production API exposure |
| 25 | Admin back office | planned / not started | API / admin / risk / liquidity | Phase 21-24 | Critical | Yes | Yes | No | RBAC, lookups, approval queues | production admin ops |
| 26 | Risk engine / kill switch | planned / not started | API / admin / risk / liquidity | Phase 16, 20, 22, 25 | Critical | Yes | Yes | No | risk limits, halts, kill switch | production risk controls |
| 27 | Market maker controls | planned / not started | API / admin / risk / liquidity | Phase 24-26 | High | Yes | Yes | No | quote limits and liquidity governance | market-maker operations |
| 28 | Futures contract foundation | planned / not started | futures / margin expansion | Phase 20, 24, 26 | High | Yes | Yes | No | contracts and margin modes | futures launch |
| 29 | Position / PnL / margin engine | planned / not started | futures / margin expansion | Phase 13, 14, 19, 28 | Critical | Yes | Yes | No | positions, margin, funding | margin trading |
| 30 | Liquidation / ADL / insurance | planned / not started | futures / margin expansion | Phase 28-29 | Critical | Yes | Yes | No | liquidation and insurance fund | leverage safety |
| 31 | Margin lending system | planned / not started | futures / margin expansion | Phase 13-15, 26 | Critical | Yes | Yes | No | borrow, repay, interest | lending launch |
| 32 | Reconciliation / compensation | planned / not started | reconciliation / security / infra / launch readiness | Phase 14, 19-22 | Critical | Yes | Yes | No | cross-domain reconciliation and compensation | silent repair |
| 33 | Security / compliance hardening | planned / not started | reconciliation / security / infra / launch readiness | Phase 24-26, 32 | Critical | Yes | Yes | No | secrets, abuse detection, compliance hooks | security hardening completed |
| 34 | Observability / SRE / incident response | planned / not started | reconciliation / security / infra / launch readiness | Phase 19-22, 32-33 | High | No | Yes | No | logs, metrics, tracing, runbooks | incident readiness |
| 35 | Production infra / CI-CD / release | planned / not started | reconciliation / security / infra / launch readiness | Phase 12-34 | High | Yes | Yes | No | build, deploy, rollback, backup, DR | production deploy readiness |
| 36 | Pre-launch certification | planned / not started | reconciliation / security / infra / launch readiness | Phase 12-35 | Critical | Yes | Yes | No | go/no-go, legal, support, launch rehearsal | production launch ready |

## Phase Groups

- Phase 0-8: Frontend / early foundation
- Phase 9-10: Backend skeleton / stubs
- Phase 11: Production architecture reset
- Phase 12-15: Funds safety / ledger foundation
- Phase 16-20: Spot trading core
- Phase 21-23: Wallet / treasury
- Phase 24-27: API / admin / risk / liquidity operations
- Phase 28-31: Futures / margin expansion
- Phase 32-36: Reconciliation / security / infra / launch readiness

## Detailed Phase Definitions

### Phase 12 - Production Database Schema & Migration

- Status: planned / not started
- Goal: create the production schema for accounts, assets, networks, symbols, balances, journals, lines, reservations, orders, fills, deposits, withdrawals, reconciliation, and admin audit.
- Why this phase exists: nothing else can safely mutate money-like state without a durable schema.
- Dependencies: Phase 11.
- Scope: DDL, migration ordering, constraints, indexes, migration tests.
- Non-goals: runtime ledger, freeze, matching, settlement, wallet chain runtime.
- Required deliverables: migration files, schema docs, migration tests.
- Acceptance criteria: schema supports later phases without fake tables or placeholder logic.
- Required tests: migration validation and schema smoke checks.
- Files / modules likely affected: `server/src/main/resources/db/migration`, schema docs.
- Data model impact: introduces authoritative financial tables.
- API impact: none yet.
- Security impact: strong constraints and auditability at the data layer.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: ledger engine completed, balance mutation completed, freeze completed, matching completed, settlement completed, production trading completed.
- Next phase handoff: Phase 13 builds on the schema only.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 12 only, do not add runtime logic, validate build/tests, mark pending human review only.

### Phase 13 - Double-Entry Ledger Engine

- Status: planned / not started
- Goal: implement immutable double-entry posting with idempotency and reversal.
- Why this phase exists: ledger correctness is the foundation for every user-funds flow.
- Dependencies: Phase 12.
- Scope: debit/credit validation, journal immutability, concurrency handling, audit trail.
- Non-goals: reservation, matching, settlement, wallet runtime.
- Required deliverables: ledger engine, tests.
- Acceptance criteria: balanced journals only; no negative mutation path.
- Required tests: posting, idempotency, reversal, concurrency.
- Files / modules likely affected: ledger domain, repositories, migrations.
- Data model impact: journals and lines become authoritative.
- API impact: ledger-facing internal APIs only.
- Security impact: immutable financial history.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: freeze, spot order flow, matching, settlement.
- Next phase handoff: Phase 14 projection/reconciliation uses the journal trail.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 13 only, do not touch later phases, validate build/tests, mark pending human review only.

### Phase 14 - Balance Projection & Ledger Reconciliation

- Status: planned / not started
- Goal: derive rebuildable balance projections from journals and compare them with ledger state.
- Why this phase exists: user-facing balance views must be provably consistent.
- Dependencies: Phase 12-13.
- Scope: projection rebuild, imbalance detection, stuck-journal detection, audit reports.
- Non-goals: automatic repair, matching, settlement, wallet runtime.
- Required deliverables: projection jobs, reconciliation reports, tests.
- Acceptance criteria: ledger-vs-balance checks are deterministic and reviewable.
- Required tests: rebuild, mismatch detection, audit output.
- Files / modules likely affected: ledger/read model services, reconciliation jobs.
- Data model impact: balance projection tables or views.
- API impact: read-only reporting.
- Security impact: prevents silent state drift.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: freeze completed, production spot order flow completed, matching completed, settlement completed.
- Next phase handoff: Phase 15 uses the projection to move funds between available and locked.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 14 only, no auto-repair, validate build/tests, mark pending human review only.

### Phase 15 - Asset Reservation / Freeze Engine

- Status: planned / not started
- Goal: implement reserve, release, commit, rollback, locked balance, and available balance.
- Why this phase exists: fund freeze is the control point for trading and withdrawal safety.
- Dependencies: Phase 12-14.
- Scope: partial fill handling, cancel release, idempotent reservation events, stuck reservation detection.
- Non-goals: matching, settlement, wallet runtime.
- Required deliverables: reservation engine, tests, state machine docs.
- Acceptance criteria: available + locked = total; idempotent reserve lifecycle.
- Required tests: reserve/release/commit/rollback, partial fill, cancel, stuck reservation.
- Files / modules likely affected: reservation domain, ledger hooks, order service boundaries.
- Data model impact: reservation state tables.
- API impact: internal reservation boundary only.
- Security impact: prevents negative balance and unauthorized release.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: production spot order flow completed, matching completed, settlement completed, production trading completed.
- Next phase handoff: Phase 16 orchestrates order flow on top of this engine.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 15 only, do not jump to orders or matching, validate build/tests, mark pending human review only.

### Phase 16 - Production Spot Order Service

- Status: planned / not started
- Goal: implement order validation, required-funds calculation, reservation, persistence, submit/cancel, and query APIs.
- Why this phase exists: the exchange needs a real order orchestration layer before matching integration.
- Dependencies: Phase 12-15.
- Scope: client-order-id idempotency, lifecycle state, no fake matching.
- Non-goals: local matching, settlement runtime, market-data runtime.
- Required deliverables: order service, persistence, tests.
- Acceptance criteria: order flow is durable and reservation-backed.
- Required tests: validation, idempotency, submit/cancel, query.
- Files / modules likely affected: spot service, controllers, repositories, DTOs.
- Data model impact: orders table and status tracking.
- API impact: spot order endpoints.
- Security impact: request validation and idempotency.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: matching completed, settlement completed, production market data completed, production trading completed.
- Next phase handoff: Phase 17 provides the C++ authority.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 16 only, do not fake matching, validate build/tests, mark pending human review only.

### Phase 17 - C++ Matching Core

- Status: planned / not started
- Goal: build deterministic matching, order book, price-time priority, replay, and snapshot recovery.
- Why this phase exists: production matching must be authoritative and deterministic.
- Dependencies: Phase 11 boundary plus Phase 16 integration target.
- Scope: limit/market order handling, cancel, partial fills, benchmark, C++ tests.
- Non-goals: balance mutation, settlement, wallet operations.
- Required deliverables: C++ core, tests, benchmark evidence.
- Acceptance criteria: deterministic replay and snapshot recovery are reproducible.
- Required tests: C++ unit/integration/benchmark tests.
- Files / modules likely affected: `core/` or `matching-core/`.
- Data model impact: event and sequence metadata.
- API impact: matching boundary protocol.
- Security impact: authority isolation.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: Java integration completed, settlement completed, production market data completed, production trading completed.
- Next phase handoff: Phase 18 integrates Java with the C++ core.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 17 only, no ledger or wallet code, validate build/tests, mark pending human review only.

### Phase 18 - Java ↔ C++ Core Integration

- Status: planned / not started
- Goal: connect Java order orchestration to the C++ matching core with command/event protocols.
- Why this phase exists: the exchange needs a safe integration boundary between orchestration and matching.
- Dependencies: Phase 16-17.
- Scope: submit/cancel, fill consumer, sequence guarantees, replay handling, circuit breaker.
- Non-goals: reimplement matching, settlement, wallet runtime.
- Required deliverables: integration layer, protocol docs, tests.
- Acceptance criteria: duplicate and replay scenarios are handled safely.
- Required tests: integration, replay, duplicate, backpressure.
- Files / modules likely affected: Java adapter, protocol DTOs, matching client.
- Data model impact: event sequencing and dedupe markers.
- API impact: internal integration protocol.
- Security impact: strict boundary separation.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: settlement completed, production market data completed, production trading completed.
- Next phase handoff: Phase 19 settles authoritative fills.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 18 only, do not create fake matching, validate build/tests, mark pending human review only.

### Phase 19 - Trade Settlement Engine

- Status: planned / not started
- Goal: settle authoritative fill events into ledger entries, fees, reserve commit, and unused reserve release.
- Why this phase exists: fills must become durable financial state.
- Dependencies: Phase 13-18.
- Scope: settlement journal, idempotency, failed-settlement compensation, final order state update.
- Non-goals: matching, market data, wallet runtime.
- Required deliverables: settlement engine, tests.
- Acceptance criteria: fill settlement is idempotent and audit-safe.
- Required tests: fill idempotency, fee, reserve commit/release, failure compensation.
- Files / modules likely affected: settlement service, order state, ledger hooks.
- Data model impact: settlement journal and settlement status.
- API impact: event consumer and settlement reporting.
- Security impact: financial correctness and auditability.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: production market data completed, real deposit completed, real withdrawal completed, production trading completed.
- Next phase handoff: Phase 20 derives public market data from authoritative events.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 19 only, do not touch matching or wallet runtime, validate build/tests, mark pending human review only.

### Phase 20 - Production Market Data Pipeline

- Status: planned / not started
- Goal: publish authoritative snapshots, deltas, trade tape, ticker, and kline.
- Why this phase exists: public market data must follow authoritative events, not UI state.
- Dependencies: Phase 17-19.
- Scope: Redis cache, WebSocket fanout, REST market API, gap handling, recovery.
- Non-goals: matching, wallet mutation, settlement logic.
- Required deliverables: market data pipeline, tests, docs.
- Acceptance criteria: snapshots and deltas are sequence-safe and recoverable.
- Required tests: snapshot, delta, gap recovery, fanout.
- Files / modules likely affected: market-data services, cache adapters, WS/SSE.
- Data model impact: market data projections.
- API impact: market REST and streaming endpoints.
- Security impact: read-only public surface hardening.
- User funds impact: no direct mutation.
- Risk level: High.
- Review gate: mandatory human review.
- Cannot claim yet: real deposit completed, real withdrawal completed, production wallet completed, launch readiness completed.
- Next phase handoff: Phase 21 starts wallet ingress.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 20 only, do not invent market state, validate build/tests, mark pending human review only.

### Phase 21 - Production Deposit System

- Status: planned / not started
- Goal: detect chain deposits, confirm them, and credit via the ledger.
- Why this phase exists: user assets must enter the exchange through a real deposit boundary.
- Dependencies: Phase 12-14.
- Scope: address generation, chain scanner boundary, confirmation policy, reorg handling.
- Non-goals: withdrawal runtime, treasury sweep logic.
- Required deliverables: deposit system, tests.
- Acceptance criteria: confirmed deposits are idempotently credited.
- Required tests: detection, confirmation, reorg, idempotent credit.
- Files / modules likely affected: wallet deposit services, ledger posting hooks.
- Data model impact: deposit status and chain references.
- API impact: deposit address and status APIs.
- Security impact: chain and address integrity.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: real withdrawal completed, treasury completed, production wallet completed, launch readiness completed.
- Next phase handoff: Phase 22 allows controlled outbound movement.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 21 only, do not add withdrawal runtime, validate build/tests, mark pending human review only.

### Phase 22 - Production Withdrawal System

- Status: planned / not started
- Goal: process withdrawal requests with reserve, review, fee deduction, broadcast, and release on failure.
- Why this phase exists: outbound user assets need a secure and auditable flow.
- Dependencies: Phase 12-15, 21.
- Scope: whitelist, risk review, tx status tracking, failed-withdrawal release.
- Non-goals: treasury policy automation beyond the boundary.
- Required deliverables: withdrawal system, tests.
- Acceptance criteria: withdrawals require policy checks and durable state.
- Required tests: available-balance, approval, broadcast, failure release.
- Files / modules likely affected: wallet withdrawal services, admin review hooks.
- Data model impact: withdrawal requests and tx tracking.
- API impact: withdrawal request and status APIs.
- Security impact: anti-abuse and approval enforcement.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: treasury completed, production wallet completed, launch readiness completed.
- Next phase handoff: Phase 23 manages inventory and custody.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 22 only, do not bypass approvals, validate build/tests, mark pending human review only.

### Phase 23 - Hot / Cold Wallet Treasury

- Status: planned / not started
- Goal: manage hot and cold wallet custody, sweeps, thresholds, batching, and signer boundary.
- Why this phase exists: wallet operations need custody controls and treasury monitoring.
- Dependencies: Phase 21-22.
- Scope: hot wallet threshold, withdrawal batching, HSM/MPC placeholder boundary, treasury reconciliation.
- Non-goals: public trading runtime.
- Required deliverables: treasury controls, tests.
- Acceptance criteria: custody inventory and alerting are auditable.
- Required tests: threshold, sweep, batch, reconciliation.
- Files / modules likely affected: treasury services, wallet adapters.
- Data model impact: custody balances and sweep logs.
- API impact: internal treasury operations.
- Security impact: signer boundary and custody safety.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: full production launch readiness, compliance hardening completed, disaster recovery readiness completed.
- Next phase handoff: Phase 24 exposes production API boundaries.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 23 only, do not add trading logic, validate build/tests, mark pending human review only.

### Phase 24 - Production Open API

- Status: planned / not started
- Goal: expose authenticated production APIs with signing, rate limiting, and scoped permissions.
- Why this phase exists: external users and market makers need safe API access.
- Dependencies: Phase 16, 20, wallet prerequisites where applicable.
- Scope: API keys, signature, timestamp/nonce, IP whitelist, order/account/market/withdraw APIs.
- Non-goals: unrestricted withdrawals, unfinished futures APIs.
- Required deliverables: Open API layer, tests.
- Acceptance criteria: scope and security boundaries are enforced.
- Required tests: auth, signature, rate limit, restricted withdraw.
- Files / modules likely affected: API auth and controller layers.
- Data model impact: API key registry and audit logs.
- API impact: public Open API endpoints.
- Security impact: request authenticity and abuse resistance.
- User funds impact: yes.
- Risk level: High.
- Review gate: mandatory human review.
- Cannot claim yet: admin back-office completed, full risk controls completed, market maker operational controls completed, launch readiness completed.
- Next phase handoff: Phase 25 adds human operations control.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 24 only, do not broaden API scope, validate build/tests, mark pending human review only.

### Phase 25 - Admin Back Office

- Status: planned / not started
- Goal: provide admin RBAC, lookup, review, adjustment requests, and audit logging.
- Why this phase exists: a real exchange needs maker-checker operations.
- Dependencies: Phase 21-24.
- Scope: user lookup, deposit/withdraw review, four-eyes approval, reason codes.
- Non-goals: silent balance mutation.
- Required deliverables: admin back office, tests.
- Acceptance criteria: admin actions are auditable and permissioned.
- Required tests: RBAC, approval workflow, audit log, reason code.
- Files / modules likely affected: admin controllers, UI, audit persistence.
- Data model impact: admin audit trail and approval records.
- API impact: admin endpoints.
- Security impact: least privilege and dual control.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: full risk engine completed, liquidity controls completed, launch readiness completed.
- Next phase handoff: Phase 26 can enforce platform-wide kill switches.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 25 only, do not bypass audit or approval, validate build/tests, mark pending human review only.

### Phase 26 - Risk Engine & Kill Switch

- Status: planned / not started
- Goal: enforce user, symbol, and global limits plus halts and kill switches.
- Why this phase exists: risk controls must be able to stop loss and abuse rapidly.
- Dependencies: Phase 16, 20, 22, 25.
- Scope: order size, price band, fat finger, withdrawal pause, symbol halt, matching halt, global kill switch.
- Non-goals: settlement logic.
- Required deliverables: risk engine, tests.
- Acceptance criteria: risk decisions are auditable and enforceable.
- Required tests: limit, pause, halt, kill-switch.
- Files / modules likely affected: risk services, order and wallet guards.
- Data model impact: risk policy tables and audit logs.
- API impact: risk control endpoints.
- Security impact: platform abuse containment.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: production risk controls completed.
- Next phase handoff: Phase 27 supports liquidity programs.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 26 only, do not add market maker behavior, validate build/tests, mark pending human review only.

### Phase 27 - Market Maker / Liquidity Controls

- Status: planned / not started
- Goal: govern market-maker permissions, quote limits, STP, and liquidity monitoring.
- Why this phase exists: liquid markets require controlled liquidity programs.
- Dependencies: Phase 24-26.
- Scope: internal liquidity config, external MM support, maker fee tiers, wash-trading detection.
- Non-goals: reimplement matching or settlement.
- Required deliverables: liquidity controls, tests.
- Acceptance criteria: quote and inventory limits are enforceable.
- Required tests: STP, quote-limit, wash-trading detection.
- Files / modules likely affected: MM config, risk and admin services.
- Data model impact: liquidity policy tables.
- API impact: MM endpoints and controls.
- Security impact: abuse prevention.
- User funds impact: yes.
- Risk level: High.
- Review gate: mandatory human review.
- Cannot claim yet: market-maker operations completed.
- Next phase handoff: Phase 28 introduces futures contract definitions.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 27 only, do not loosen risk boundaries, validate build/tests, mark pending human review only.

### Phase 28 - Futures Contract Foundation

- Status: planned / not started
- Goal: define futures contracts, tick/lot size, funding interval, index/mark price, leverage, margin mode, and risk tiers.
- Why this phase exists: futures must be defined before position accounting exists.
- Dependencies: Phase 20, 24, 26.
- Scope: contract metadata and risk tier boundaries.
- Non-goals: positions, PnL, liquidation logic.
- Required deliverables: contract foundation, tests.
- Acceptance criteria: contract metadata is versioned and auditable.
- Required tests: definition validation and boundary checks.
- Files / modules likely affected: futures contract registry, APIs.
- Data model impact: futures contract definitions.
- API impact: contract metadata APIs.
- Security impact: contract governance.
- User funds impact: yes.
- Risk level: High.
- Review gate: mandatory human review.
- Cannot claim yet: futures launch.
- Next phase handoff: Phase 29 uses contract definitions for position accounting.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 28 only, do not add position logic, validate build/tests, mark pending human review only.

### Phase 29 - Position / PnL / Margin Engine

- Status: planned / not started
- Goal: manage position lifecycle, realized/unrealized PnL, margin modes, leverage adjustment, and funding settlement.
- Why this phase exists: futures trading requires authoritative position accounting.
- Dependencies: Phase 13, 14, 19, 28.
- Scope: isolated/cross margin, leverage, funding settlement.
- Non-goals: liquidation and ADL logic.
- Required deliverables: position engine, tests.
- Acceptance criteria: position math is deterministic and auditable.
- Required tests: open/close, PnL, margin, funding.
- Files / modules likely affected: position services, margin ledger hooks.
- Data model impact: positions, margin, funding records.
- API impact: futures position APIs.
- Security impact: financial correctness.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: margin trading.
- Next phase handoff: Phase 30 handles insolvency and forced closure.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 29 only, do not add liquidation code, validate build/tests, mark pending human review only.

### Phase 30 - Liquidation / ADL / Insurance Fund

- Status: planned / not started
- Goal: implement liquidation triggers, partial liquidation, bankruptcy handling, insurance fund, and ADL.
- Why this phase exists: leveraged products need insolvency controls.
- Dependencies: Phase 28-29.
- Scope: liquidation order, simulation, chaos tests, auditability.
- Non-goals: margin lending.
- Required deliverables: liquidation engine, tests.
- Acceptance criteria: liquidation is deterministic and explainable.
- Required tests: trigger, partial liquidation, ADL queue, chaos/simulation.
- Files / modules likely affected: liquidation services, insurance fund records.
- Data model impact: liquidation and insurance ledgers.
- API impact: liquidation reporting.
- Security impact: protects exchange solvency.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: leveraged-risk safety completed.
- Next phase handoff: Phase 31 introduces lending/borrowing.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 30 only, do not skip auditability, validate build/tests, mark pending human review only.

### Phase 31 - Margin Lending System

- Status: planned / not started
- Goal: implement borrow, repay, interest accrual, collateral valuation, forced repayment, and bad debt handling.
- Why this phase exists: margin lending expands exchange product surface with controlled credit risk.
- Dependencies: Phase 13-15, 26.
- Scope: lending ledger and borrow limits.
- Non-goals: liquidation engine.
- Required deliverables: margin lending system, tests.
- Acceptance criteria: lending is bounded by collateral and risk policy.
- Required tests: borrow, repay, interest, forced repayment.
- Files / modules likely affected: lending services, risk policies.
- Data model impact: lending ledger and interest accrual.
- API impact: margin lending APIs.
- Security impact: credit risk containment.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: lending launch.
- Next phase handoff: Phase 32 reconciles cross-domain state.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 31 only, do not add liquidation or new product surfaces, validate build/tests, mark pending human review only.

### Phase 32 - Reconciliation / Compensation

- Status: planned / not started
- Goal: reconcile ledger, orders, matching events, wallet state, deposits, withdrawals, and fees.
- Why this phase exists: real operations need mismatch detection and approved compensation.
- Dependencies: Phase 14, 19-22.
- Scope: stuck-state detector, compensation workflow, approval gates.
- Non-goals: automatic silent repair.
- Required deliverables: reconciliation engine, tests.
- Acceptance criteria: mismatches open cases and require review.
- Required tests: ledger-vs-balance, order-vs-trade, chain-vs-wallet, compensation path.
- Files / modules likely affected: reconciliation jobs, audit reports.
- Data model impact: case records and compensation logs.
- API impact: reconciliation reporting.
- Security impact: protects against silent state corruption.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: production reconciliation completed.
- Next phase handoff: Phase 33 hardens security and compliance.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 32 only, do not auto-fix state, validate build/tests, mark pending human review only.

### Phase 33 - Security / Compliance Hardening

- Status: planned / not started
- Goal: harden secrets management, abuse detection, sanctions/KYC hooks, and pen-test remediation.
- Why this phase exists: regulated operations need explicit security and compliance controls.
- Dependencies: Phase 24-26, 32.
- Scope: threat model, dependency audit, suspicious withdrawal alerts, admin anomaly detection.
- Non-goals: product expansion.
- Required deliverables: security hardening changes, tests, remediation list.
- Acceptance criteria: critical findings are tracked and addressed.
- Required tests: security regression and dependency audit.
- Files / modules likely affected: auth, audit, security adapters.
- Data model impact: security events and compliance records.
- API impact: security/compliance hooks.
- Security impact: direct and primary.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: security hardening completed.
- Next phase handoff: Phase 34 adds operational visibility.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 33 only, do not add launch claims, validate build/tests, mark pending human review only.

### Phase 34 - Observability / SRE / Incident Response

- Status: planned / not started
- Goal: add structured logs, metrics, tracing, dashboards, alerts, runbooks, severity policy, and postmortem template.
- Why this phase exists: production operations need measurable response capability.
- Dependencies: Phase 19-22, 32-33.
- Scope: order latency, matching latency, wallet alerts, ledger imbalance alerts.
- Non-goals: product features.
- Required deliverables: telemetry and incident-response docs, tests.
- Acceptance criteria: operators can detect and respond to incidents.
- Required tests: telemetry and alert validation, runbook drill.
- Files / modules likely affected: logging, metrics, alerting, docs.
- Data model impact: incident records and dashboards.
- API impact: operational endpoints only.
- Security impact: visibility into abuse and failure.
- User funds impact: no direct mutation.
- Risk level: High.
- Review gate: mandatory human review.
- Cannot claim yet: incident readiness completed.
- Next phase handoff: Phase 35 uses the observability baseline for release control.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 34 only, do not add product logic, validate build/tests, mark pending human review only.

### Phase 35 - Production Infra / CI-CD / Release

- Status: planned / not started
- Goal: create production build, deployment, rollback, backup, restore, and disaster-recovery controls.
- Why this phase exists: launch requires operationally safe release mechanics.
- Dependencies: Phase 12-34.
- Scope: Docker production build, Kubernetes or deployment manifests, environment separation.
- Non-goals: product logic changes.
- Required deliverables: infra and release pipeline, tests or drills.
- Acceptance criteria: release can be rolled back and restored.
- Required tests: backup/restore and DR drills.
- Files / modules likely affected: deployment assets, CI config, runbooks.
- Data model impact: release metadata.
- API impact: none or deployment endpoints.
- Security impact: secure secret injection and environment separation.
- User funds impact: yes.
- Risk level: High.
- Review gate: mandatory human review.
- Cannot claim yet: production deploy readiness.
- Next phase handoff: Phase 36 certifies launch readiness.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 35 only, do not claim launch readiness, validate build/tests, mark pending human review only.

### Phase 36 - Pre-Launch Certification

- Status: planned / not started
- Goal: complete fee schedule, revenue reporting, listing policy, support workflow, legal terms, privacy, risk disclosure, SLA, agreements, bug bounty, launch rehearsal, and go/no-go review.
- Why this phase exists: launch must be business-ready, legally ready, and operationally ready.
- Dependencies: Phase 12-35.
- Scope: certification and launch rehearsal.
- Non-goals: runtime feature expansion.
- Required deliverables: launch pack and explicit sign-off evidence.
- Acceptance criteria: go/no-go can be signed only after all prior gates pass.
- Required tests: launch rehearsal, restore drill, support drill.
- Files / modules likely affected: business docs, ops docs, release checklist.
- Data model impact: none or launch metadata.
- API impact: none or launch gating only.
- Security impact: external trust and compliance readiness.
- User funds impact: yes.
- Risk level: Critical.
- Review gate: mandatory human review.
- Cannot claim yet: production launch ready until explicit sign-off.
- Next phase handoff: none; Phase 36 is the launch gate.
- Codex implementation prompt: reload repo, read `docs/PHASE_REVIEW_WORKFLOW.md`, implement Phase 36 only, do not claim launch readiness without sign-off, validate build/tests, mark pending human review only.

