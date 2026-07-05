# Phase 26 - Risk Engine & Kill Switch

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Implement production risk gates for users, symbols, global operations, and emergency stops across trading and wallet flows.

## Why this phase exists

Even correct trading and wallet logic is unsafe without enforceable pre-trade and operational limits plus hard-stop controls for incidents.

## Dependencies

- Previous phases required: Phase 16, Phase 20, Phase 22, Phase 25
- External dependencies if any: risk policy inputs, incident policy, alerting destinations
- Blocking risks: fail-open risk decisions, missing kill-switch propagation, inconsistent rule evaluation

## Scope

- User risk limits
- Symbol risk limits
- Global risk limits
- Order size limit
- Price band
- Fat finger protection
- Withdrawal pause
- Symbol halt
- Matching halt
- Global kill switch
- Risk audit

## Non-goals

- Liquidation engine
- Portfolio margin logic
- Compliance-only controls outside defined scope

## Required deliverables

- Risk decision service
- Configurable user, symbol, and global risk rules
- Halt and pause controls
- Global kill-switch propagation
- Risk audit trail
- Risk test suite

## Acceptance criteria

- Unsafe orders are blocked before reserve or submit paths advance
- Withdrawal pause and symbol halt are enforceable
- Matching halt and global kill switch fail closed
- Risk decisions are logged and reviewable
- Fat-finger and price-band protections are deterministic

## Required tests

- User/symbol/global limit tests
- Order-size and price-band tests
- Fat-finger protection tests
- Withdrawal pause tests
- Symbol halt and matching halt tests
- Global kill-switch tests
- Risk audit-log tests

## Files / modules likely affected

- `server/src/main/java/com/lumix/risk/`
- `server/src/main/java/com/lumix/spot/`
- `server/src/main/java/com/lumix/wallet/`
- admin and config packages

## Data model impact

- May add risk rule tables, policy versions, halt-state tables, and risk audit metadata

## API impact

- Affects order and wallet request behavior
- May add admin-facing risk configuration APIs

## Security impact

- Critical
- Must not fail open under outage, stale config, or partial propagation

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because risk failure can allow unsafe trades or withdrawals

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: this phase is part of the production safety control plane

## Cannot claim yet

- liquidity controls completed
- futures and margin expansion completed
- launch readiness completed

## Next phase handoff

Phase 27 adds market-maker and liquidity controls on top of the risk and API foundations.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/ARCHITECTURE_PRODUCTION.md, and docs/phases/PHASE_26_RISK_ENGINE_KILL_SWITCH.md.

Goal: implement Phase 26 only - Risk Engine & Kill Switch.
Scope: user/symbol/global risk limits, order size limit, price band, fat finger protection, withdrawal pause, symbol halt, matching halt, global kill switch, and risk audit.
Non-goals: liquidation engine, portfolio margin, later phases.
Deliverables: production risk engine, tests, and progress/doc updates tied to real implementation.
Tests: risk limit checks, price-band and fat-finger protection, withdrawal pause, symbol/matching halts, global kill switch, risk audit, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 26 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: liquidity controls completed, futures and margin expansion completed, launch readiness completed.
Final output format: Changed Files, Summary, What Phase 26 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
