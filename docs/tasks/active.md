# Active AI Work Registry

This file is the shared starting point for parallel AI work. Update it before starting implementation work, commit it, and push it so other agents can see the active lane before they choose a task.

Keep entries short. Long notes belong in `docs/tasks/handoffs/`.

## Protocol

1. Pull the latest branch state.
2. Run `./shells/ai-context.sh` and `git status --short`.
3. Read this registry before selecting work.
4. Add or update one row with `doing`, expected file areas, and a timestamp.
5. Commit and push only the registry claim before coding.
6. Start implementation only after the claim push succeeds.
7. When done, update the row to `done` or remove it in the implementation commit. If unfinished, keep it as `doing` or `blocked` and add a handoff note.

If context is lost, the next agent must read this file and `docs/tasks/handoffs/` before deciding whether to resume or start another task.

## Status Legend

- `doing`: actively owned by an agent.
- `blocked`: owned but waiting on an external decision, environment fix, or dependency.
- `handoff`: unfinished and ready for another agent to resume.
- `done`: completed; remove after the completion commit lands unless short-term visibility is useful.

## Active Work

| Status | Task / Lane | Owner | Since | Expected Areas | Handoff |
| --- | --- | --- | --- | --- | --- |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#polymarket-integration | T1 polymarket-schema-versioning | 2026-06-03 | `src/main/java/com/example/exchange/domain/model/dto`, `src/main/java/com/example/exchange/domain/service/Polymarket*`, `src/test/java/com/example/exchange/domain/service/Polymarket*`, `docs/ai/maps/polymarket-security.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#observability-matching-metrics | T2 observability-matching-metrics | 2026-06-03 | `src/main/java/com/example/exchange/application/service/OperationalMetricsService.java`, `src/main/java/com/example/exchange/domain/model/dto/OperationalMetricsSnapshot.java`, `src/test/java/com/example/exchange/application/service/OperationalMetricsServiceTest.java`, `docs/en/observability.md`, `docs/zh-TW/observability.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#market-data-gateway-scaling-notes | T1 market-data-gateway | 2026-06-03 | `docs/en/market-data-gateway-scaling.md`, `docs/zh-TW/market-data-gateway-scaling.md`, `docs/en/todo.md`, `docs/zh-TW/todo.md`, `docs/en/current-state.md`, `docs/zh-TW/current-state.md`, `docs/tasks/production-readiness/02-p1-production-hardening.md`, `docs/ai/maps/reliability-market-data.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#market-maker-quote-api-frequency-limit-policy | T1 market-maker-api | 2026-06-03 | `src/main/java/com/example/exchange/interfaces/web/controller/MarketMakerController.java`, `src/main/java/com/example/exchange/interfaces/web/security`, `src/main/java/com/example/exchange/infra/config`, `src/test/java/com/example/exchange/interfaces/web/security`, `docs/ai/maps/market-maker-hedging.md`, `docs/tasks/production-readiness/02-p1-production-hardening.md`, `docs/en/todo.md`, `docs/zh-TW/todo.md` |  |

Replace the `_none_` row with real claim rows when work starts.

## Claim Row Template

Use one row per terminal agent. Keep `Expected Areas` narrow enough that another agent can safely choose unrelated work.

```markdown
| doing | docs/tasks/core-kernel/01-replayable-matching-core.md | T1 matching-command-log | 2026-06-02 | `src/main/java/com/example/exchange/infra/matching`, `src/test/java/com/example/exchange/infra/matching`, `docs/ai/maps/order-matching.md` |  |
```

Terminal labels should be stable for the session, for example `T1 matching-command-log` or `T2 polymarket-clob-state`.

## Conflict Notes

If a new lane needs files already listed by another `doing` row:

1. Do not start implementation.
2. Split the work into a smaller task file or define a dependency order.
3. If useful discovery already happened, write a handoff note under `docs/tasks/handoffs/`.
