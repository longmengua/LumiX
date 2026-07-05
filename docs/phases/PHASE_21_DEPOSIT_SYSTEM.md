# Phase 21 - Production Deposit System

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Implement the production inbound deposit system from address issuance through confirmation handling and idempotent ledger credit.

## Why this phase exists

Real user funds cannot enter the exchange safely without address generation, chain observation, confirmation policy, reorg handling, and idempotent credit posting.

## Dependencies

- Previous phases required: Phase 12, Phase 13, Phase 14
- External dependencies if any: address provider, chain scanner or indexer, supported network policies
- Blocking risks: double credit, missed deposits, reorg mishandling, unsafe callback trust

## Scope

- Address generation
- Chain scanner / indexer boundary
- Confirmation policy
- Deposit detection
- Reorg handling
- Idempotent credit
- Ledger posting
- Deposit status lifecycle
- Manual review

## Non-goals

- Withdrawal broadcast
- Treasury sweep strategy
- Full wallet operations platform beyond deposits

## Required deliverables

- Deposit address service
- Chain observation or callback boundary
- Confirmation and reorg policy
- Idempotent deposit credit flow
- Deposit status lifecycle
- Manual review path for exceptions
- Deposit test suite

## Acceptance criteria

- Deposit addresses can be issued safely for supported assets and networks
- Confirmed deposits credit exactly once
- Reorg or insufficient-confirmation behavior is defined and tested
- Exceptional deposits can be routed to manual review
- Deposit history is auditable end to end

## Required tests

- Address generation tests
- Detection and confirmation tests
- Reorg handling tests
- Idempotent credit tests
- Deposit status lifecycle tests
- Manual review routing tests

## Files / modules likely affected

- `server/src/main/java/com/lumix/wallet/`
- `server/src/main/java/com/lumix/ledger/`
- admin review packages

## Data model impact

- Uses deposit, address, and network tables from Phase 12
- May add callback security or scanner checkpoint metadata

## API impact

- Enables deposit address and deposit-history APIs to become production-backed

## Security impact

- Must authenticate callbacks or secure scanner ingestion
- Must protect against replay, spoofed deposit notifications, and wrong-network credit

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because this phase directly credits user funds

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: incorrect deposit crediting directly affects user assets

## Cannot claim yet

- real withdrawal completed
- treasury completed
- production wallet completed
- launch readiness completed

## Next phase handoff

Phase 22 implements outbound withdrawal request, review, broadcast boundary, and failed-withdrawal release behavior.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/ARCHITECTURE_PRODUCTION.md, docs/FUNDS_SAFETY_MODEL.md, and docs/phases/PHASE_21_DEPOSIT_SYSTEM.md.

Goal: implement Phase 21 only - Production Deposit System.
Scope: address generation, chain scanner/indexer boundary, confirmation policy, deposit detection, reorg handling, idempotent credit, ledger posting, deposit status lifecycle, and manual review.
Non-goals: withdrawal runtime, treasury sweep, later phases.
Deliverables: production deposit system, tests, and progress/doc updates tied to real implementation.
Tests: address generation, detection/confirmation, reorg handling, idempotent credit, status lifecycle, manual review, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 21 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: real withdrawal completed, treasury completed, production wallet completed, launch readiness completed.
Final output format: Changed Files, Summary, What Phase 21 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
