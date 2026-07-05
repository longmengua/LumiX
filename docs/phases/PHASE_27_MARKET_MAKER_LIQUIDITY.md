# Phase 27 - Market Maker / Liquidity Controls

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Implement production operational controls for internal and external liquidity providers without weakening market integrity.

## Why this phase exists

Once trading is live, liquidity operations need explicit API permissions, quote limits, self-trade prevention, wash-trading detection, and inventory controls.

## Dependencies

- Previous phases required: Phase 24, Phase 25, Phase 26
- External dependencies if any: market-maker onboarding rules, surveillance expectations
- Blocking risks: abusive quoting, self-trading, inventory blowout, weak maker permissions

## Scope

- Market maker API
- Internal liquidity config
- External market maker support
- Quote limits
- Self-trade prevention
- Wash trading detection
- Maker fee tier
- Inventory limit
- Liquidity monitoring

## Non-goals

- Full internal trading strategy implementation
- Manipulative or opaque market intervention
- Futures market-maker expansion before futures phases complete

## Required deliverables

- Market-maker permission and onboarding model
- Quote-limit controls
- Self-trade-prevention behavior
- Wash-trading detection hooks
- Maker fee-tier logic
- Inventory-limit monitoring
- Liquidity monitoring dashboards or metrics
- Market-maker control test suite

## Acceptance criteria

- Market-maker API access is permissioned and auditable
- Quote and inventory limits are enforceable
- Self-trade prevention is defined and testable
- Wash-trading alerts can be generated
- Liquidity monitoring exposes operational health and abuse indicators

## Required tests

- Market-maker permission tests
- Quote-limit tests
- Self-trade-prevention tests
- Wash-trading detection tests
- Maker fee-tier tests
- Inventory-limit tests
- Liquidity monitoring tests

## Files / modules likely affected

- market-maker packages
- `server/src/main/java/com/lumix/openapi/`
- `server/src/main/java/com/lumix/risk/`
- monitoring packages

## Data model impact

- May add maker profiles, fee-tier metadata, quote-limit configs, and surveillance event records

## API impact

- Extends or specializes Open API permissions and controls for market makers

## Security impact

- Must prevent privilege misuse through maker credentials
- Must preserve surveillance and audit trails for suspicious liquidity behavior

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because liquidity-control failures can destabilize markets and indirectly harm user execution

## Risk level

- High

## Review gate

- Mandatory human review before merge: Yes
- Why: this phase controls privileged actors in live markets

## Cannot claim yet

- futures contract foundation completed
- position/pnl/margin completed
- launch readiness completed

## Next phase handoff

Phase 28 begins futures and margin product expansion with contract definitions and configuration boundaries.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/TRADING_CORE_BOUNDARIES.md, and docs/phases/PHASE_27_MARKET_MAKER_LIQUIDITY.md.

Goal: implement Phase 27 only - Market Maker / Liquidity Controls.
Scope: market-maker API permissions, internal liquidity config, external maker support, quote limits, self-trade prevention, wash-trading detection, maker fee tier, inventory limits, and liquidity monitoring.
Non-goals: full internal strategy implementation, futures expansion, later phases.
Deliverables: liquidity-control implementation, tests, and progress/doc updates tied to real implementation.
Tests: maker permissions, quote limits, self-trade prevention, wash-trading detection, fee tier, inventory limits, monitoring, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 27 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: futures contract foundation completed, position/pnl/margin completed, launch readiness completed.
Final output format: Changed Files, Summary, What Phase 27 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
