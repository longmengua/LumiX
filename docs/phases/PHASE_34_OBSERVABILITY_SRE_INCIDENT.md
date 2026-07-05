# Phase 34 - Observability / SRE / Incident Response

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Make the exchange observable and operable under incident conditions with structured logs, metrics, tracing, dashboards, alerts, runbooks, and severity policy.

## Why this phase exists

Live trading and live funds systems require fast diagnosis, on-call procedures, and operational evidence beyond functional correctness.

## Dependencies

- Previous phases required: Phase 19, Phase 20, Phase 21, Phase 22, Phase 32, Phase 33
- External dependencies if any: metrics stack, log stack, tracing stack, paging destination
- Blocking risks: blind failures, alert noise, no runbooks, unclear incident ownership

## Scope

- Structured logs
- Metrics
- Tracing
- Order latency dashboard
- Matching latency dashboard
- Wallet alert
- Ledger imbalance alert
- Reconciliation alert
- On-call runbook
- Incident severity policy
- Postmortem template

## Non-goals

- Infrastructure deployment itself
- Legal or customer communications policy beyond incident hooks
- Launch sign-off itself

## Required deliverables

- Structured logging standard
- Metrics and tracing coverage
- Critical dashboards
- Wallet, ledger, and reconciliation alerts
- On-call runbooks
- Incident severity matrix
- Postmortem template
- Observability validation tests or drills

## Acceptance criteria

- Core order, matching, wallet, ledger, and reconciliation flows emit usable telemetry
- Critical alerts map to runbooks
- Latency dashboards exist for order and matching paths
- Incident severity and escalation policy are documented
- Postmortem template is ready for use

## Required tests

- Telemetry emission tests
- Alert-rule tests where practical
- Dashboard data validation
- Runbook drill or tabletop validation
- Incident escalation policy review

## Files / modules likely affected

- telemetry and config packages
- ops docs
- alert and dashboard configuration
- possibly CI or deployment metadata

## Data model impact

- Minimal business-model impact
- May add event or metric tags, correlation IDs, and incident metadata

## API impact

- No major user-facing API scope change
- Improves traceability and supportability of existing APIs

## Security impact

- Must avoid leaking secrets or sensitive user data in logs and traces
- Must protect observability backends appropriately

## User funds impact

- No direct balance mutation
- Review requirements: mandatory human review before merge because missing or unsafe telemetry can block incident handling for funds and trading systems

## Risk level

- High

## Review gate

- Mandatory human review before merge: Yes
- Why: operational readiness is a launch gate, not a background nicety

## Cannot claim yet

- production infra/release completed
- launch readiness completed

## Next phase handoff

Phase 35 creates deploy, rollback, environment, backup, and disaster-recovery execution paths.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, and docs/phases/PHASE_34_OBSERVABILITY_SRE_INCIDENT.md.

Goal: implement Phase 34 only - Observability / SRE / Incident Response.
Scope: structured logs, metrics, tracing, order and matching latency dashboards, wallet/ledger/reconciliation alerts, on-call runbooks, incident severity policy, and postmortem template.
Non-goals: infrastructure deployment itself, launch sign-off, later phases.
Deliverables: observability and incident-response implementation, checks or drills, and progress/doc updates tied to real implementation.
Tests: telemetry emission, alerts, dashboard validation, runbook drill/tabletop, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 34 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: production infra/release completed, launch readiness completed.
Final output format: Changed Files, Summary, What Phase 34 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
