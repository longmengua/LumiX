# Phase 28 - Futures Contract Foundation

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Define the production futures contract model and core configuration boundaries needed before position and margin logic can exist.

## Why this phase exists

Futures expansion cannot begin safely without canonical contract definition, price granularity, funding cadence, leverage configuration, and risk-tier structure.

## Dependencies

- Previous phases required: Phase 20, Phase 24, Phase 26
- External dependencies if any: index-price source policy, contract listing policy
- Blocking risks: ambiguous contract rules, inconsistent mark/index configuration, unsafe leverage defaults

## Scope

- Contract definition
- Tick size
- Lot size
- Funding interval
- Index price
- Mark price
- Leverage config
- Margin mode config
- Risk limit tier

## Non-goals

- Position engine
- PnL engine
- Liquidation engine
- Live leveraged trading rollout

## Required deliverables

- Futures contract metadata model
- Tick and lot-size configuration
- Funding-interval configuration
- Index and mark-price boundary definitions
- Leverage and margin-mode configuration model
- Risk-tier configuration
- Futures foundation test suite

## Acceptance criteria

- Every futures contract has explicit trading and risk configuration
- Tick size, lot size, leverage, and risk-tier rules are versioned and testable
- Index and mark-price dependency boundaries are explicit
- No live futures trading path bypasses later phases

## Required tests

- Contract-definition tests
- Tick and lot-size validation tests
- Funding-interval configuration tests
- Leverage and margin-mode config tests
- Risk-tier config tests

## Files / modules likely affected

- future `server/src/main/java/com/lumix/futures/`
- `server/src/main/java/com/lumix/market/`
- config and docs packages

## Data model impact

- Adds futures contract metadata, leverage config, margin modes, and risk-tier tables

## API impact

- May expose read-only futures contract metadata APIs
- No live leveraged execution claim yet

## Security impact

- Must prevent invalid or unsafe contract configuration rollout
- Must audit contract-definition changes

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because futures config errors propagate into leveraged risk

## Risk level

- High

## Review gate

- Mandatory human review before merge: Yes
- Why: futures configuration becomes the base for later leveraged funds logic

## Cannot claim yet

- position/pnl/margin completed
- liquidation completed
- margin lending completed
- launch readiness completed

## Next phase handoff

Phase 29 implements position lifecycle, PnL, and margin accounting on top of these contract definitions.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/ARCHITECTURE_PRODUCTION.md, and docs/phases/PHASE_28_FUTURES_CONTRACT_FOUNDATION.md.

Goal: implement Phase 28 only - Futures Contract Foundation.
Scope: contract definition, tick size, lot size, funding interval, index price boundary, mark price boundary, leverage config, margin mode config, and risk limit tiers.
Non-goals: position/PnL/margin runtime, liquidation, live leveraged trading, later phases.
Deliverables: futures contract foundation, tests, and progress/doc updates tied to real implementation.
Tests: contract config, tick/lot validation, funding interval, leverage/margin-mode config, risk tiers, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 28 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: position/pnl/margin completed, liquidation completed, margin lending completed, launch readiness completed.
Final output format: Changed Files, Summary, What Phase 28 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
