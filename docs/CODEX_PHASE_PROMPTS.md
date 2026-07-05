# CODEX Phase Prompts

Use these prompts as the authoritative starting point for future Codex phase implementation. Each prompt is phase-limited and must not be expanded into later phases.

## Phase 12 - Production Database Schema & Migration

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_12_DATABASE_SCHEMA.md.
Phase goal: implement Phase 12 only - Production Database Schema & Migration.
Scope: only production schema and migration work for accounts, assets, asset networks, symbols, account balances, ledger journals, ledger lines, reservations, orders, trades/fills, deposits, withdrawals, reconciliation, and admin audit.
Non-goals: do not implement Phase 13+ runtime logic; do not add fake ledger, matching, freeze, settlement, deposit, or withdraw logic.
Deliverables: migration files, schema docs, migration tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_12_DATABASE_SCHEMA.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_12_DATABASE_SCHEMA.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: ledger engine completed, balance mutation completed, freeze completed, matching completed, settlement completed, production trading completed.
Final output format: Changed Files; Summary; What Phase 12 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 13 - Double-Entry Ledger Engine

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_13_DOUBLE_ENTRY_LEDGER.md.
Phase goal: implement Phase 13 only - Double-Entry Ledger Engine.
Scope: only double-entry posting, debit/credit validation, journal immutability, idempotency, reversal model, projection update boundary, negative-balance prevention, concurrency handling, and ledger tests.
Non-goals: do not implement reservation runtime, matching, settlement, wallet runtime, or later phases.
Deliverables: ledger implementation, persistence layer, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_13_DOUBLE_ENTRY_LEDGER.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_13_DOUBLE_ENTRY_LEDGER.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: order freeze completed, spot order production flow completed, matching completed, settlement completed.
Final output format: Changed Files; Summary; What Phase 13 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 14 - Balance Projection & Ledger Reconciliation

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_14_BALANCE_RECONCILIATION.md.
Phase goal: implement Phase 14 only - Balance Projection & Ledger Reconciliation.
Scope: only journal-to-balance projection, rebuild tooling, reconciliation runs, ledger-vs-balance checks, imbalance detection, stuck-journal detection, and audit reports.
Non-goals: do not implement reservation runtime, matching, settlement, wallet runtime, or automatic asset repair.
Deliverables: projection and reconciliation implementation, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_14_BALANCE_RECONCILIATION.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_14_BALANCE_RECONCILIATION.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: freeze completed, production spot order flow completed, matching completed, settlement completed.
Final output format: Changed Files; Summary; What Phase 14 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 15 - Asset Reservation / Freeze Engine

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_15_ASSET_RESERVATION.md.
Phase goal: implement Phase 15 only - Asset Reservation / Freeze Engine.
Scope: only reserve, release, commit, rollback, locked and available balances, partial fill support, cancel release, idempotent reservation events, and stuck-reservation detection.
Non-goals: do not implement spot order orchestration, matching, settlement, wallet runtime, or later phases.
Deliverables: reservation engine, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_15_ASSET_RESERVATION.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_15_ASSET_RESERVATION.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: production spot order flow completed, matching completed, settlement completed, production trading completed.
Final output format: Changed Files; Summary; What Phase 15 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 16 - Production Spot Order Service

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_16_PRODUCTION_SPOT_ORDER.md.
Phase goal: implement Phase 16 only - Production Spot Order Service.
Scope: only order validation, required-funds calculation, reservation, persistence, submit/cancel through MatchingEngineClient, lifecycle rules, client-order-id idempotency, and query APIs.
Non-goals: do not implement fake Java matching, matching core runtime, settlement runtime, market-data runtime, or later phases.
Deliverables: production spot order service, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_16_PRODUCTION_SPOT_ORDER.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_16_PRODUCTION_SPOT_ORDER.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: matching completed, settlement completed, production market data completed, production trading completed.
Final output format: Changed Files; Summary; What Phase 16 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 17 - C++ Matching Core

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_17_CPP_MATCHING_CORE.md.
Phase goal: implement Phase 17 only - C++ Matching Core.
Scope: only deterministic matching, price-time priority, order book, limit and market orders, cancel, partial fill, sequence numbers, replay, snapshot, crash recovery, benchmark, and C++ tests.
Non-goals: do not implement ledger mutation, wallet operations, settlement logic, or later phases.
Deliverables: core source tree, tests, benchmarks, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_17_CPP_MATCHING_CORE.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_17_CPP_MATCHING_CORE.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: run the core build/test commands you add; cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: Java integration completed, settlement completed, production market data completed, production trading completed.
Final output format: Changed Files; Summary; What Phase 17 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 18 - Java ↔ C++ Core Integration

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_18_MATCHING_INTEGRATION.md.
Phase goal: implement Phase 18 only - Java ↔ C++ Core Integration.
Scope: only command protocol, event protocol, submit/cancel integration, fill-event consumer, sequence guarantees, duplicate handling, replay handling, backpressure, circuit breaker, and integration tests.
Non-goals: do not reimplement the matching core, settlement runtime, market-data runtime, wallet runtime, or later phases.
Deliverables: matching integration layer, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_18_MATCHING_INTEGRATION.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_18_MATCHING_INTEGRATION.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: run core validation as needed; cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: settlement completed, production market data completed, production trading completed.
Final output format: Changed Files; Summary; What Phase 18 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 19 - Trade Settlement Engine

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_19_TRADE_SETTLEMENT.md.
Phase goal: implement Phase 19 only - Trade Settlement Engine.
Scope: only fill-event settlement, maker/taker fee calculation, reserve commit, unused reserve release, ledger entries, settlement journal, idempotency, failed-settlement compensation, and final order-state update.
Non-goals: do not implement matching, market-data runtime, wallet runtime, or later phases.
Deliverables: settlement engine, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_19_TRADE_SETTLEMENT.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_19_TRADE_SETTLEMENT.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: run core validation as needed; cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: production market data completed, real deposit completed, real withdrawal completed, production trading completed.
Final output format: Changed Files; Summary; What Phase 19 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 20 - Production Market Data Pipeline

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_20_MARKET_DATA_PIPELINE.md.
Phase goal: implement Phase 20 only - Production Market Data Pipeline.
Scope: only order-book snapshot and delta, trade tape, ticker, kline, Redis cache, WebSocket fanout, REST market API, sequence-gap handling, and recovery.
Non-goals: do not implement matching, balance mutation, wallet runtime, or later phases.
Deliverables: market-data pipeline, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_20_MARKET_DATA_PIPELINE.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_20_MARKET_DATA_PIPELINE.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: real deposit completed, real withdrawal completed, production wallet completed, launch readiness completed.
Final output format: Changed Files; Summary; What Phase 20 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 21 - Production Deposit System

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_21_DEPOSIT_SYSTEM.md.
Phase goal: implement Phase 21 only - Production Deposit System.
Scope: only address generation, chain scanner/indexer boundary, confirmation policy, deposit detection, reorg handling, idempotent credit, ledger posting, deposit status lifecycle, and manual review.
Non-goals: do not implement withdrawal runtime, treasury sweep logic, or later phases.
Deliverables: deposit system, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_21_DEPOSIT_SYSTEM.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_21_DEPOSIT_SYSTEM.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: real withdrawal completed, treasury completed, production wallet completed, launch readiness completed.
Final output format: Changed Files; Summary; What Phase 21 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 22 - Production Withdrawal System

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_22_WITHDRAWAL_SYSTEM.md.
Phase goal: implement Phase 22 only - Production Withdrawal System.
Scope: only withdrawal request, available-balance check, fund reservation, approval workflow, risk review, address whitelist, fee deduction, broadcast boundary, tx tracking, and failed-withdrawal release.
Non-goals: do not implement treasury strategy, HSM/MPC vendor runtime, or later phases.
Deliverables: withdrawal system, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_22_WITHDRAWAL_SYSTEM.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_22_WITHDRAWAL_SYSTEM.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: treasury completed, production wallet completed, launch readiness completed.
Final output format: Changed Files; Summary; What Phase 22 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 23 - Hot / Cold Wallet Treasury

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_23_HOT_COLD_WALLET_TREASURY.md.
Phase goal: implement Phase 23 only - Hot / Cold Wallet Treasury.
Scope: only hot/cold wallet roles, sweep strategy, thresholds, withdrawal batching, signer boundary, HSM/MPC placeholder boundary, treasury reconciliation, and wallet alerting.
Non-goals: do not implement public trading runtime or later phases.
Deliverables: treasury controls, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_23_HOT_COLD_WALLET_TREASURY.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_23_HOT_COLD_WALLET_TREASURY.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: full production launch readiness, compliance hardening completed, disaster recovery readiness completed.
Final output format: Changed Files; Summary; What Phase 23 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 24 - Production Open API

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_24_PRODUCTION_OPEN_API.md.
Phase goal: implement Phase 24 only - Production Open API.
Scope: only API key management, permission scopes, HMAC/RSA signature, timestamp/nonce, IP whitelist, rate limit, order/account/market APIs, restricted withdraw APIs, and API audit logs.
Non-goals: do not enable unrestricted withdraw API, unfinished futures or margin APIs, or later phases.
Deliverables: production Open API layer, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_24_PRODUCTION_OPEN_API.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_24_PRODUCTION_OPEN_API.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: admin back-office completed, full risk controls completed, market maker operational controls completed, launch readiness completed.
Final output format: Changed Files; Summary; What Phase 24 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 25 - Admin Back Office

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_25_ADMIN_BACK_OFFICE.md.
Phase goal: implement Phase 25 only - Admin Back Office.
Scope: only admin RBAC, user/account/order/trade lookup, deposit/withdraw review, asset-adjustment requests, four-eyes approval, admin audit log, and reason codes.
Non-goals: do not implement silent direct balance mutation, ad-hoc DB edits as an ops substitute, or later phases.
Deliverables: admin back office, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_25_ADMIN_BACK_OFFICE.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_25_ADMIN_BACK_OFFICE.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: full risk engine completed, liquidity controls completed, launch readiness completed.
Final output format: Changed Files; Summary; What Phase 25 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 26 - Risk Engine & Kill Switch

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_26_RISK_ENGINE_KILL_SWITCH.md.
Phase goal: implement Phase 26 only - Risk Engine & Kill Switch.
Scope: only user/symbol/global risk limits, order size limit, price band, fat finger protection, withdrawal pause, symbol halt, matching halt, global kill switch, and risk audit.
Non-goals: do not implement liquidation engine, portfolio margin expansion, or later phases.
Deliverables: risk engine and kill switch, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_26_RISK_ENGINE_KILL_SWITCH.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_26_RISK_ENGINE_KILL_SWITCH.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: liquidity controls completed, futures and margin expansion completed, launch readiness completed.
Final output format: Changed Files; Summary; What Phase 26 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 27 - Market Maker / Liquidity Controls

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_27_MARKET_MAKER_LIQUIDITY.md.
Phase goal: implement Phase 27 only - Market Maker / Liquidity Controls.
Scope: only market-maker API permissions, internal liquidity config, external maker support, quote limits, self-trade prevention, wash-trading detection, maker fee tier, inventory limits, and liquidity monitoring.
Non-goals: do not implement full internal trading strategy, futures expansion, or later phases.
Deliverables: liquidity-control layer, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_27_MARKET_MAKER_LIQUIDITY.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_27_MARKET_MAKER_LIQUIDITY.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: futures contract foundation completed, position/pnl/margin completed, launch readiness completed.
Final output format: Changed Files; Summary; What Phase 27 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 28 - Futures Contract Foundation

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_28_FUTURES_CONTRACT_FOUNDATION.md.
Phase goal: implement Phase 28 only - Futures Contract Foundation.
Scope: only contract definition, tick size, lot size, funding interval, index-price boundary, mark-price boundary, leverage config, margin mode config, and risk limit tiers.
Non-goals: do not implement position/PnL/margin runtime, liquidation, live leveraged trading, or later phases.
Deliverables: futures contract foundation, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_28_FUTURES_CONTRACT_FOUNDATION.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_28_FUTURES_CONTRACT_FOUNDATION.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: position/pnl/margin completed, liquidation completed, margin lending completed, launch readiness completed.
Final output format: Changed Files; Summary; What Phase 28 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 29 - Position / PnL / Margin Engine

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_29_POSITION_PNL_MARGIN.md.
Phase goal: implement Phase 29 only - Position / PnL / Margin Engine.
Scope: only position open/close, realized/unrealized PnL, initial and maintenance margin, isolated/cross margin, leverage adjustment, and funding payment settlement.
Non-goals: do not implement liquidation, ADL, insurance fund, margin lending, or later phases.
Deliverables: position and margin engine, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_29_POSITION_PNL_MARGIN.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_29_POSITION_PNL_MARGIN.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: liquidation completed, ADL completed, insurance fund completed, margin lending completed, launch readiness completed.
Final output format: Changed Files; Summary; What Phase 29 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 30 - Liquidation / ADL / Insurance Fund

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_30_LIQUIDATION_ADL_INSURANCE.md.
Phase goal: implement Phase 30 only - Liquidation / ADL / Insurance Fund.
Scope: only liquidation triggers, partial liquidation, bankruptcy price, liquidation orders, insurance fund, ADL queue, bad debt handling, liquidation audit, simulation tests, and chaos tests.
Non-goals: do not implement margin lending or later phases.
Deliverables: liquidation and ADL system, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_30_LIQUIDATION_ADL_INSURANCE.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_30_LIQUIDATION_ADL_INSURANCE.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: margin lending completed, full reconciliation and compensation completed, launch readiness completed.
Final output format: Changed Files; Summary; What Phase 30 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 31 - Margin Lending System

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_31_MARGIN_LENDING.md.
Phase goal: implement Phase 31 only - Margin Lending System.
Scope: only borrow, repay, interest accrual, collateral valuation, margin level, forced repayment, borrow limits, lending ledger, and bad debt handling.
Non-goals: do not implement futures liquidation or later phases.
Deliverables: margin lending system, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_31_MARGIN_LENDING.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_31_MARGIN_LENDING.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: full reconciliation and compensation completed, security/compliance hardening completed, launch readiness completed.
Final output format: Changed Files; Summary; What Phase 31 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 32 - Reconciliation & Compensation System

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_32_RECONCILIATION_COMPENSATION.md.
Phase goal: implement Phase 32 only - Reconciliation & Compensation System.
Scope: only ledger-vs-balance, order-vs-trade, matching-event-vs-DB, wallet-vs-chain, deposit/withdrawal, fee-revenue reconciliation, stuck-state detection, compensation workflow, and no automatic asset repair without approval.
Non-goals: do not implement silent asset repair or later phases.
Deliverables: reconciliation and compensation system, tests, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_32_RECONCILIATION_COMPENSATION.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_32_RECONCILIATION_COMPENSATION.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: security/compliance hardening completed, observability/SRE completed, production infra/release completed, launch readiness completed.
Final output format: Changed Files; Summary; What Phase 32 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 33 - Security / Compliance Hardening

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_33_SECURITY_COMPLIANCE_HARDENING.md.
Phase goal: implement Phase 33 only - Security / Compliance Hardening.
Scope: only threat model, secrets management, API abuse detection, KYC/AML integration boundary, sanctions screening hook, suspicious withdrawal alert, device/session risk, admin anomaly detection, dependency audit, and pen-test fix list.
Non-goals: do not implement launch sign-off or later phases.
Deliverables: security and compliance hardening work, checks, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_33_SECURITY_COMPLIANCE_HARDENING.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_33_SECURITY_COMPLIANCE_HARDENING.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: observability/SRE completed, production infra/release completed, launch readiness completed.
Final output format: Changed Files; Summary; What Phase 33 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 34 - Observability / SRE / Incident Response

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_34_OBSERVABILITY_SRE_INCIDENT.md.
Phase goal: implement Phase 34 only - Observability / SRE / Incident Response.
Scope: only structured logs, metrics, tracing, order and matching latency dashboards, wallet/ledger/reconciliation alerts, on-call runbooks, incident severity policy, and postmortem template.
Non-goals: do not implement production deployment itself, launch sign-off, or later phases.
Deliverables: observability and incident-response layer, drills or checks, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_34_OBSERVABILITY_SRE_INCIDENT.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_34_OBSERVABILITY_SRE_INCIDENT.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: production infra/release completed, launch readiness completed.
Final output format: Changed Files; Summary; What Phase 34 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 35 - Production Infra / CI-CD / Release

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_35_PRODUCTION_INFRA_CICD_RELEASE.md.
Phase goal: implement Phase 35 only - Production Infra / CI-CD / Release.
Scope: only Docker production build, deployment manifests, environment separation, secret injection, DB migration pipeline, rollback strategy, canary/blue-green deploy, backup/restore drill, and disaster-recovery drill.
Non-goals: do not implement business launch approval or later phases.
Deliverables: infra and release system, drill evidence, and progress updates tied to actual implementation.
Tests: run the required tests listed in docs/phases/PHASE_35_PRODUCTION_INFRA_CICD_RELEASE.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_35_PRODUCTION_INFRA_CICD_RELEASE.md, and only directly related roadmap/progress docs if implementation reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script; run any new infra validation commands added for this phase.
Cannot claim yet: pre-launch certification completed, production launch ready.
Final output format: Changed Files; Summary; What Phase 35 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```

## Phase 36 - Pre-Launch Certification & Business Readiness

```text
Reload the repo from disk. Read: AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, docs/phases/PHASE_36_PRE_LAUNCH_CERTIFICATION.md.
Phase goal: implement Phase 36 only - Pre-Launch Certification & Business Readiness.
Scope: only fee schedule, revenue report, listing policy, customer support workflow, legal terms, privacy policy, risk disclosure, withdrawal SLA, market maker agreement, bug bounty, launch rehearsal, and go/no-go review.
Non-goals: do not implement new runtime features or later phases.
Deliverables: final certification package, rehearsal evidence, and progress updates tied to actual readiness work.
Tests: run the required tests listed in docs/phases/PHASE_36_PRE_LAUNCH_CERTIFICATION.md plus project build validation.
Docs to update: AI_PROGRESS.md, docs/phases/PHASE_36_PRE_LAUNCH_CERTIFICATION.md, and only directly related roadmap/progress docs if readiness reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if package.json contains a test script.
Cannot claim yet: production launch ready until this phase fully passes with explicit human sign-off.
Final output format: Changed Files; Summary; What Phase 36 completed; What is still NOT completed; Validation Results; Next Recommended Command.
```
