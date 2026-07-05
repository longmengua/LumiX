# Phase 33 - Security / Compliance Hardening

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Harden the platform against secret compromise, API abuse, suspicious operations, and compliance gaps before launch.

## Why this phase exists

Even correct business logic is not launch-ready without threat modeling, secret handling, abuse detection, compliance hooks, and security remediation tracking.

## Dependencies

- Previous phases required: Phase 24, Phase 25, Phase 26, Phase 32
- External dependencies if any: KYC/AML provider boundaries, sanctions screening source, security review input
- Blocking risks: poor secret hygiene, missing abuse detection, incomplete compliance integration, unresolved critical findings

## Scope

- Threat model
- Secrets management
- API abuse detection
- KYC / AML integration boundary
- Sanctions screening hook
- Suspicious withdrawal alert
- Device / session risk
- Admin anomaly detection
- Dependency audit
- Penetration test fix list

## Non-goals

- Full legal or policy authoring
- Launch sign-off itself
- Replacing the runtime business engines

## Required deliverables

- Threat model document
- Secret-management plan and integration points
- Abuse-detection controls
- KYC/AML and sanctions hook boundaries
- Suspicious-withdrawal alerting
- Device/session risk controls
- Admin anomaly-detection hooks
- Dependency audit results
- Pen-test remediation backlog

## Acceptance criteria

- Critical secret and abuse paths are hardened
- Compliance integration boundaries are explicit
- Suspicious withdrawal and admin anomaly alerts are actionable
- Dependency audit and pen-test fix list exist with tracked severity
- Critical unresolved security gaps are visible before launch decisions

## Required tests

- Secret handling tests where applicable
- API abuse or throttling tests
- Alert-generation tests for suspicious withdrawal and admin anomalies
- Dependency-audit execution
- Security regression or hardening checks

## Files / modules likely affected

- security and auth packages
- wallet and admin packages
- config and ops docs
- dependency manifests and CI hooks

## Data model impact

- May add security event records, compliance-case metadata, and anomaly alerts

## API impact

- Hardens API and session behavior
- Adds compliance and abuse hooks around existing APIs

## Security impact

- Critical
- This phase is itself a security-hardening phase

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because security defects can become direct fund-loss paths

## Risk level

- Critical

## Review gate

- Mandatory human review before merge: Yes
- Why: security and compliance are pre-launch blockers

## Cannot claim yet

- observability/SRE completed
- production infra/release completed
- launch readiness completed

## Next phase handoff

Phase 34 adds structured observability, runbooks, alerting, incident policy, and postmortem infrastructure.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, and docs/phases/PHASE_33_SECURITY_COMPLIANCE_HARDENING.md.

Goal: implement Phase 33 only - Security / Compliance Hardening.
Scope: threat model, secrets management, API abuse detection, KYC/AML integration boundary, sanctions screening hook, suspicious withdrawal alert, device/session risk, admin anomaly detection, dependency audit, and pen-test fix list.
Non-goals: launch sign-off, later phases.
Deliverables: security/compliance hardening work, tests/checks, and progress/doc updates tied to real implementation.
Tests: security regressions, abuse detection, alert generation, dependency audit, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 33 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: observability/SRE completed, production infra/release completed, launch readiness completed.
Final output format: Changed Files, Summary, What Phase 33 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
