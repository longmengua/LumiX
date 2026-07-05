# Phase 36 - Pre-Launch Certification & Business Readiness

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Perform the final technical, operational, business, and policy certification required before any production launch claim.

## Why this phase exists

Even when core systems are implemented, launch still requires fee, legal, support, market-maker, SLA, and rehearsal readiness plus a final go/no-go decision.

## Dependencies

- Previous phases required: Phase 12 through Phase 35
- External dependencies if any: legal review, support staffing, market-maker agreements, executive launch sign-off
- Blocking risks: unresolved critical issues, incomplete policies, failed rehearsal, unsupported customer operations

## Scope

- Fee schedule
- Revenue report
- Listing policy
- Customer support workflow
- Legal terms
- Privacy policy
- Risk disclosure
- Withdrawal SLA
- Market maker agreement
- Bug bounty
- Launch rehearsal
- Go / no-go review

## Non-goals

- New runtime feature implementation
- Unreviewed scope expansion after readiness review begins

## Required deliverables

- Final fee schedule
- Revenue and business reporting baseline
- Listing policy
- Customer-support and escalation workflow
- Legal terms, privacy policy, and risk disclosure
- Withdrawal SLA
- Market-maker agreement readiness
- Bug-bounty readiness
- Launch rehearsal report
- Go/no-go review package

## Acceptance criteria

- All prior critical phases are complete and reviewed
- Launch rehearsal succeeds or produces resolved follow-up items
- Legal, support, and market-operation documents exist
- Go/no-go review has explicit owners and sign-off path
- No production launch claim is made before this phase passes

## Required tests

- Launch rehearsal
- End-to-end operational checklist validation
- Support escalation drill
- Go/no-go review evidence collection

## Files / modules likely affected

- launch and ops docs
- support and legal document directories
- release metadata

## Data model impact

- Little or no direct runtime data-model change
- May add readiness evidence records or checklists if tracked in repo

## API impact

- No major new API implementation
- Final policy and SLA constraints may shape public documentation

## Security impact

- Must confirm security, compliance, and incident controls are already satisfied
- Must ensure no unresolved critical security findings remain open

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because this phase is the final production-launch gate for user funds and trading safety

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: this phase is the final launch gate and cannot be auto-approved

## Cannot claim yet

- nothing further can be claimed before this phase passes
- production launch ready

## Next phase handoff

There is no subsequent implementation phase. Successful completion leads only to an explicit launch decision, not automatic launch.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/CODEX_PHASE_PROMPTS.md, and docs/phases/PHASE_36_PRE_LAUNCH_CERTIFICATION.md.

Goal: implement Phase 36 only - Pre-Launch Certification & Business Readiness.
Scope: fee schedule, revenue report, listing policy, customer support workflow, legal terms, privacy policy, risk disclosure, withdrawal SLA, market maker agreement, bug bounty, launch rehearsal, and go/no-go review.
Non-goals: new runtime features, later phases.
Deliverables: final certification package, launch rehearsal evidence, and progress/doc updates tied to actual readiness work.
Tests: launch rehearsal, operational checklist validation, support drill, go/no-go evidence collection, and normal build validation to confirm repo health.
Docs to update: AI_PROGRESS.md and the Phase 36 doc only if readiness reality changes.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: production launch ready until this phase fully passes with explicit human sign-off.
Final output format: Changed Files, Summary, What Phase 36 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
