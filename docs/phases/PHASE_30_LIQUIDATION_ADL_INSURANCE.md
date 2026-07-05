# Phase 30 - Liquidation / ADL / Insurance Fund

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Implement deterministic liquidation, ADL, bankruptcy handling, and insurance-fund workflows for leveraged products.

## Why this phase exists

Futures and margin products are unsafe without controlled forced-exit behavior, bad-debt handling, and replayable liquidation logic.

## Dependencies

- Previous phases required: Phase 28, Phase 29
- External dependencies if any: approved liquidation policy, simulation environment
- Blocking risks: incorrect trigger logic, unfair ADL ordering, broken bankruptcy handling, unsafe insurance-fund accounting

## Scope

- Liquidation trigger
- Partial liquidation
- Bankruptcy price
- Liquidation order
- Insurance fund
- ADL queue
- Bad debt handling
- Liquidation audit
- Simulation tests
- Chaos tests

## Non-goals

- Margin lending
- General compensation workflows outside liquidation scope
- Broader launch readiness work

## Required deliverables

- Liquidation decision engine
- Partial-liquidation rules
- Bankruptcy-price logic
- Insurance-fund accounting path
- ADL queue model
- Liquidation audit trail
- Simulation and chaos test coverage

## Acceptance criteria

- Liquidation triggers are reproducible and auditable
- Partial liquidation and bankruptcy handling follow explicit rules
- Insurance-fund movements are journaled
- ADL queue behavior is deterministic
- Simulation and chaos tests cover stressed scenarios

## Required tests

- Liquidation-trigger tests
- Partial-liquidation tests
- Bankruptcy-price tests
- Insurance-fund accounting tests
- ADL queue tests
- Simulation tests
- Chaos tests

## Files / modules likely affected

- liquidation packages under `server/src/main/java/com/lumix/`
- `server/src/main/java/com/lumix/futures/`
- `server/src/main/java/com/lumix/ledger/`
- risk packages

## Data model impact

- Adds liquidation events, ADL queue state, insurance-fund records, and bad-debt tracking

## API impact

- Enables liquidation history and risk-status views
- No full margin-lending or launch-readiness claim yet

## Security impact

- Must protect liquidation controls from manual abuse
- Must preserve auditable forensic history for every forced-exit path

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because liquidation defects can rapidly harm many accounts

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: liquidation is one of the highest-risk trading systems

## Cannot claim yet

- margin lending completed
- full reconciliation and compensation completed
- launch readiness completed

## Next phase handoff

Phase 31 adds borrow, repay, collateral valuation, interest accrual, and bad-debt handling for margin lending.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/ARCHITECTURE_PRODUCTION.md, and docs/phases/PHASE_30_LIQUIDATION_ADL_INSURANCE.md.

Goal: implement Phase 30 only - Liquidation / ADL / Insurance Fund.
Scope: liquidation trigger, partial liquidation, bankruptcy price, liquidation orders, insurance fund, ADL queue, bad debt handling, liquidation audit, simulation tests, and chaos tests.
Non-goals: margin lending, broader launch readiness, later phases.
Deliverables: liquidation system, tests, and progress/doc updates tied to real implementation.
Tests: liquidation trigger, partial liquidation, bankruptcy price, insurance fund, ADL queue, simulation, chaos, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 30 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: margin lending completed, full reconciliation and compensation completed, launch readiness completed.
Final output format: Changed Files, Summary, What Phase 30 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
