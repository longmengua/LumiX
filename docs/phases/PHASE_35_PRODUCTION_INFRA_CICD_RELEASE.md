# Phase 35 - Production Infra / CI-CD / Release

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Implement production build, deploy, rollback, environment, backup, and release orchestration for web, server, and future core components.

## Why this phase exists

Production operation requires reproducible deployment paths, environment isolation, safe rollback, and disaster-recovery evidence in addition to application code.

## Dependencies

- Previous phases required: Phase 12 through Phase 34 as applicable to deployable components
- External dependencies if any: container registry, orchestrator, secret store, backup targets
- Blocking risks: unsafe rollout path, broken rollback, migration drift, poor environment separation

## Scope

- Docker production build
- Kubernetes / deployment manifests
- Environment separation
- Secret injection
- DB migration pipeline
- Rollback strategy
- Canary deploy
- Blue-green deploy
- Backup / restore drill
- Disaster recovery drill

## Non-goals

- Business launch approval itself
- Product policy definition
- Replacing application-level correctness testing

## Required deliverables

- Production build artifacts and container definitions
- Deployment manifests
- Environment separation model
- Secret-injection workflow
- DB migration deployment pipeline
- Rollback strategy
- Canary and blue-green rollout procedures
- Backup and restore drill evidence
- Disaster-recovery drill evidence

## Acceptance criteria

- Deployments are reproducible and versioned
- Secrets are injected safely per environment
- DB migrations are part of controlled release flow
- Rollback path is documented and tested
- Backup/restore and disaster-recovery drills produce evidence

## Required tests

- Build artifact validation
- Deployment manifest validation
- Migration pipeline validation
- Rollback drill
- Canary or blue-green rollout test
- Backup/restore drill
- Disaster-recovery drill

## Files / modules likely affected

- `infra/` or deployment directories if created
- CI configuration
- release docs
- build tooling for `server/`, `web/`, and future `core/`

## Data model impact

- No direct business-model change
- Controls how schemas and data are promoted and recovered

## API impact

- No direct API feature change
- Improves reliability of API release and rollback

## Security impact

- Must protect secrets, deployment credentials, and backup artifacts
- Must ensure environment isolation and least privilege

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because deployment and restore mistakes can affect production funds integrity

## Risk level

- High

## Review gate

- Mandatory human review before merge: Yes
- Why: release controls are part of production safety and recovery

## Cannot claim yet

- pre-launch certification completed
- production launch ready

## Next phase handoff

Phase 36 consumes technical readiness evidence and turns it into final launch certification and go/no-go decision workflow.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, and docs/phases/PHASE_35_PRODUCTION_INFRA_CICD_RELEASE.md.

Goal: implement Phase 35 only - Production Infra / CI-CD / Release.
Scope: Docker production build, deployment manifests, environment separation, secret injection, DB migration pipeline, rollback strategy, canary/blue-green deploy, backup/restore drill, and disaster recovery drill.
Non-goals: business launch approval, later phases.
Deliverables: production infra/release system, drill evidence, and progress/doc updates tied to real implementation.
Tests: build validation, manifest validation, migration pipeline validation, rollback drill, canary/blue-green test, backup/restore drill, disaster-recovery drill.
Docs to update: AI_PROGRESS.md and the Phase 35 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists; run any infra validation commands added for this phase.
Cannot claim yet: pre-launch certification completed, production launch ready.
Final output format: Changed Files, Summary, What Phase 35 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```
