<!-- File purpose: English core-v1 release freeze checklist. Chinese version: ../zh-TW/core-v1-release-checklist.md. -->
# Core V1 Release Checklist

This checklist freezes the current production-core baseline into a bounded `core-v1` release candidate. It is not a claim that the system is ready for real-money production.

Chinese version: [../zh-TW/core-v1-release-checklist.md](../zh-TW/core-v1-release-checklist.md)

## Scope Included

- Replayable matching baseline, sequencer lease/fencing, cancel-replace replay, recovery, and validation.
- Liquidation/ADL baseline with ranking, planning, scan result, decision audit, halt, and manual review controls.
- Bonus-credit ledger separation, grant consumption/expiry/clawback baseline, expiry scheduler, turnover facts, and focused reconciliation hooks.
- Auditable ledger/reconciliation baseline with trial balance, replay comparison, persisted issue workflow, and admin issue APIs.
- Market-maker profile/risk, exposure, quote checks, hedge strategy/execution baseline, fill audit, decision-vs-fill reconciliation, venue callback ingestion, and safe adapter decorators.

## Deferred

- Client/admin web applications.
- Polymarket production worker split and complete CLOB lifecycle.
- Production WebSocket/SSE gateway scaling.
- Real venue-specific hedge adapter credentials, signing, and callback verification.
- Full reporting, compliance, load testing, dashboards, tracing, and alert manager setup.

## Required Commands

```bash
./shells/ai-context.sh
./mvnw test
git diff --check
git status --short
```

## Release Gate

- [ ] `./mvnw test` passes.
- [ ] `git diff --check` passes.
- [ ] Flyway migrations are append-only and ordered.
- [ ] Scheduler defaults that can mutate state are disabled unless explicitly intended.
- [ ] Protected admin APIs are covered by `/api/market-maker/**`, `/api/recovery/**`, `/api/risk/**`, and related security classifier paths.
- [ ] Current-state and TODO docs point to the freeze task instead of new feature expansion.
- [ ] Known production blockers are documented and not silently treated as complete.

## Production Risks That Remain

- Matching worker routing still needs full production lease guard integration.
- MySQL, Redis, and Kafka transaction boundaries are not fully defined.
- Real market data durability, gateway scaling, observability backend, alerting, and load testing are not complete.
- Real external venue/bank/chain integrations require signed callbacks, idempotency, replay protection, and operator runbooks.
