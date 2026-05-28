# Task: Core V1 Freeze

Status: `done`

## Goal

Freeze the current production-core baseline into a bounded `core-v1` release candidate. This task is explicitly about stopping feature spread, documenting what is in/out, and adding only the minimum verification/runbook work needed for a reliable handoff.

## Freeze Scope

Core v1 includes only:

- Replayable matching baseline, sequencer lease/fencing, cancel-replace replay, and recovery validation.
- Liquidation/ADL baseline with ranking, planning, scan result, decision audit, and operator halt/manual-review controls.
- Bonus-credit ledger separation, grant consumption/expiry/clawback baseline, expiry scheduler, turnover facts, and focused reconciliation hooks.
- Auditable ledger/reconciliation baseline with trial balance, replay comparison, persisted issue workflow, and admin issue APIs.
- Market-maker profile/risk, exposure, quote checks, hedge strategy/execution baseline, fill audit, decision-vs-fill reconciliation, venue callback ingestion, and safe adapter decorators.

## Explicitly Out Of Scope For Core V1

- Client web and admin web implementation.
- Polymarket worker split, CLOB production lifecycle, and user WebSocket service.
- Full production WebSocket/SSE gateway scaling.
- Real exchange-specific hedge venue adapter credentials/signing beyond an interface/skeleton.
- Full compliance, KYC/AML, surveillance, reporting suite, and admin console.
- Load testing implementation, dashboards, tracing backend, and alert manager setup.

## Required Closeout Work

1. [x] Update `docs/en/todo.md` and `docs/zh-TW/todo.md` so the next work is `core-v1` freeze, not more feature expansion.
2. [x] Update `docs/en/current-state.md` and `docs/zh-TW/current-state.md` with the freeze boundary and remaining production risks.
3. [x] Add a release verification checklist with exact commands.
4. [x] Add a migration/config checklist covering Flyway, scheduler defaults, risk switches, and protected API paths.
5. [x] Add a smoke-test/runbook document for the core trading baseline.
6. [x] Keep code changes limited to compile/test fixes or missing minimal validation discovered while writing the checklist.

## Acceptance Criteria

- A new engineer or AI agent can tell what belongs to core v1 and what is deferred.
- The release checklist says exactly what commands to run before tagging or deploying.
- The current TODO no longer encourages endless expansion before freeze.
- `./mvnw test` and `git diff --check` pass after documentation updates.

## Verification Commands

```bash
./shells/ai-context.sh
./mvnw test
git diff --check
git status --short
```

## Read First

- [../../en/current-state.md](../../en/current-state.md)
- [../../en/todo.md](../../en/todo.md)
- [../../zh-TW/current-state.md](../../zh-TW/current-state.md)
- [../../zh-TW/todo.md](../../zh-TW/todo.md)
- [../../ai/code-map.md](../../ai/code-map.md)
