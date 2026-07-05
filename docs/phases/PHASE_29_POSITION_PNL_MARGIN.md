# Phase 29 - Position / PnL / Margin Engine

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Implement the production futures position, realized and unrealized PnL, and margin accounting engine.

## Why this phase exists

Futures products cannot go live without deterministic position state, margin calculations, leverage changes, and funding settlement behavior.

## Dependencies

- Previous phases required: Phase 13, Phase 14, Phase 19, Phase 28
- External dependencies if any: approved formulas, funding policy, risk-tier model
- Blocking risks: incorrect PnL math, wrong margin requirements, inconsistent isolated vs cross behavior

## Scope

- Position open / close
- Realized PnL
- Unrealized PnL
- Initial margin
- Maintenance margin
- Isolated margin
- Cross margin
- Leverage adjustment
- Funding payment settlement

## Non-goals

- Liquidation execution
- ADL
- Insurance fund operation
- Margin lending

## Required deliverables

- Position engine
- PnL calculation boundaries
- Margin calculator
- Isolated and cross margin model
- Leverage-adjustment rules
- Funding-payment settlement path
- Position and margin test suite

## Acceptance criteria

- Position state rebuilds deterministically from authoritative events
- Realized and unrealized PnL calculations are versioned and tested
- Initial and maintenance margin are enforced correctly
- Isolated and cross margin modes behave as specified
- Funding payments settle through auditable flows

## Required tests

- Position open/close tests
- Realized and unrealized PnL tests
- Initial and maintenance margin tests
- Isolated vs cross margin tests
- Leverage-adjustment tests
- Funding-payment settlement tests

## Files / modules likely affected

- `server/src/main/java/com/lumix/futures/`
- `server/src/main/java/com/lumix/ledger/`
- settlement and risk integration packages

## Data model impact

- Adds position state, margin state, funding accrual or funding-payment records, and related history tables

## API impact

- Enables futures position and margin query APIs
- Still requires liquidation and lending phases before full futures product claims

## Security impact

- Must prevent unauthorized margin-mode or leverage changes
- Must audit position and margin transitions

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because this phase directly controls leveraged account state

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: position and margin errors create immediate leveraged-loss risk

## Cannot claim yet

- liquidation completed
- ADL completed
- insurance fund completed
- margin lending completed
- launch readiness completed

## Next phase handoff

Phase 30 adds liquidation triggers, bankruptcy handling, insurance fund, and ADL workflows.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/ARCHITECTURE_PRODUCTION.md, and docs/phases/PHASE_29_POSITION_PNL_MARGIN.md.

Goal: implement Phase 29 only - Position / PnL / Margin Engine.
Scope: position open/close, realized/unrealized PnL, initial and maintenance margin, isolated/cross margin, leverage adjustment, and funding payment settlement.
Non-goals: liquidation, ADL, insurance fund, margin lending, later phases.
Deliverables: position and margin engine, tests, and progress/doc updates tied to real implementation.
Tests: position lifecycle, PnL, margin modes, leverage adjustment, funding settlement, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 29 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: liquidation completed, ADL completed, insurance fund completed, margin lending completed, launch readiness completed.
Final output format: Changed Files, Summary, What Phase 29 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
