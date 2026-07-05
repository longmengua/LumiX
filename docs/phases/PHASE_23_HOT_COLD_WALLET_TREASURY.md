# Phase 23 - Hot / Cold Wallet Treasury

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Define and implement treasury controls for hot wallets, cold wallets, sweep strategy, batching, signer boundaries, and treasury reconciliation.

## Why this phase exists

Deposit and withdrawal runtime alone are insufficient without custody controls, exposure limits, and reconciliation for platform-owned wallets.

## Dependencies

- Previous phases required: Phase 21, Phase 22
- External dependencies if any: signer system, custody provider, treasury ops policy
- Blocking risks: excessive hot-wallet exposure, poor batching control, weak signer boundary, treasury drift

## Scope

- Hot wallet
- Cold wallet
- Sweep strategy
- Hot wallet threshold
- Withdrawal batching
- Signer boundary
- HSM / MPC placeholder
- Treasury reconciliation
- Wallet alerting

## Non-goals

- Full HSM or MPC vendor implementation details if externalized
- User-facing trading logic
- Non-wallet business workflows

## Required deliverables

- Treasury wallet role model
- Sweep and refill policy
- Hot-wallet threshold rules
- Withdrawal batching policy
- Signer boundary definition
- Treasury reconciliation workflow
- Wallet alerting plan
- Treasury test coverage

## Acceptance criteria

- Hot and cold wallet responsibilities are separated
- Threshold breaches create actionable alerts
- Sweep and refill policy is explicit and testable
- Treasury reconciliation can detect internal vs chain mismatches
- Signer boundary is documented and reviewable

## Required tests

- Threshold rule tests
- Sweep strategy tests
- Withdrawal batching tests
- Treasury reconciliation tests
- Alert routing tests

## Files / modules likely affected

- `server/src/main/java/com/lumix/wallet/`
- new treasury packages
- admin and ops-support packages

## Data model impact

- May add treasury wallet inventory, sweep records, batch records, and reconciliation metadata

## API impact

- Mostly internal and admin-facing
- No broader public trading API claims

## Security impact

- Must isolate signer boundary and protect custody workflow
- Must log all treasury movements and approvals

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because treasury controls affect platform solvency and withdrawal safety

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: custody and treasury controls are launch-critical

## Cannot claim yet

- full production launch readiness
- compliance hardening completed
- disaster recovery readiness completed

## Next phase handoff

Phase 24 exposes production Open API capabilities on top of the completed core trading and wallet foundations.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/ARCHITECTURE_PRODUCTION.md, and docs/phases/PHASE_23_HOT_COLD_WALLET_TREASURY.md.

Goal: implement Phase 23 only - Hot / Cold Wallet Treasury.
Scope: hot/cold wallet model, sweep strategy, thresholds, withdrawal batching, signer boundary, HSM/MPC placeholder boundary, treasury reconciliation, and wallet alerting.
Non-goals: public trading logic, full HSM/MPC vendor implementation, later phases.
Deliverables: treasury controls, tests, and progress/doc updates tied to real implementation.
Tests: threshold, sweep, batching, treasury reconciliation, alerting, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 23 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: full production launch readiness, compliance hardening completed, disaster recovery readiness completed.
Final output format: Changed Files, Summary, What Phase 23 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
