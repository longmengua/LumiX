# Active AI Work Registry

This file is the shared starting point for parallel AI work. Update it before starting implementation work, commit it, and push it so other agents can see the active lane before they choose a task.

Keep entries short. Long notes belong in `docs/tasks/handoffs/`.

## Protocol

1. Pull the latest branch state and merge or rebase the latest `main` into the work branch before selecting work.
2. Run `./shells/ai-context.sh` and `git status --short`.
3. Read this registry before selecting work.
4. Add or update one row with `doing`, expected file areas, and a timestamp.
5. Commit and push only the registry claim before coding.
6. Merge or fast-forward that claim commit to `main` and push `main`.
7. Start implementation only after the claim is visible on `origin/main`, not only on the agent branch.
8. When done, update the row to `done` or remove it in the implementation commit, merge/push completion to `main`, then claim the next lane. If unfinished, keep it as `doing` or `blocked` and add a handoff note.

If context is lost, the next agent must read this file and `docs/tasks/handoffs/` before deciding whether to resume or start another task.

## Status Legend

- `doing`: actively owned by an agent.
- `blocked`: owned but waiting on an external decision, environment fix, or dependency.
- `handoff`: unfinished and ready for another agent to resume.
- `done`: completed; remove after the completion commit lands unless short-term visibility is useful.

## Active Work

| Status | Task / Lane | Owner | Since | Expected Areas | Handoff |
| --- | --- | --- | --- | --- | --- |
| doing | docs/tasks/production-readiness/02-p1-production-hardening.md#observability-db-redis-latency | T2 observability-db-redis-latency | 2026-06-03 | `src/main/java/com/example/exchange/application/service/OperationalMetricsService.java`, `src/main/java/com/example/exchange/domain/model/dto/OperationalMetricsSnapshot.java`, `src/test/java/com/example/exchange/application/service/OperationalMetricsServiceTest.java`, `docs/en/observability.md`, `docs/zh-TW/observability.md`, `docs/tasks/production-readiness/02-p1-production-hardening.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#archive-exporter-job-skeleton | T2 archive-exporter-skeleton | 2026-06-03 | `src/main/java/com/example/exchange/application/service/*Archive*`, `src/main/java/com/example/exchange/application/scheduler/*Archive*`, `src/main/java/com/example/exchange/infra/config/*Archive*`, `src/test/java/com/example/exchange/application/service/*Archive*`, `docs/en/archive-strategy.md`, `docs/zh-TW/archive-strategy.md`, `docs/ai/maps/persistence-tests.md`, `docs/tasks/production-readiness/02-p1-production-hardening.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#live-position-sql-mirror-index-design | T2 live-position-sql-mirror | 2026-06-03 | `docs/en/live-position-sql-mirror.md`, `docs/zh-TW/live-position-sql-mirror.md`, `docs/ai/maps/persistence-tests.md`, `docs/tasks/production-readiness/02-p1-production-hardening.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#live-order-sql-mirror-index-design | T2 live-order-sql-mirror | 2026-06-03 | `docs/en/live-order-sql-mirror.md`, `docs/zh-TW/live-order-sql-mirror.md`, `docs/ai/maps/persistence-tests.md`, `docs/tasks/production-readiness/02-p1-production-hardening.md`, `docs/en/todo.md`, `docs/zh-TW/todo.md`, `docs/en/current-state.md`, `docs/zh-TW/current-state.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#polymarket-user-websocket-checkpoint-replay | T2 polymarket-ws-checkpoint | 2026-06-03 | `src/main/java/com/example/exchange/domain/service/PolymarketUserWebSocketService.java`, `src/main/java/com/example/exchange/domain/model/entity/PredictionPolymarket*`, `src/main/java/com/example/exchange/domain/repository/jpa/PredictionPolymarket*`, `src/test/java/com/example/exchange/domain/service/Polymarket*`, `docs/ai/maps/polymarket-security.md`, `docs/tasks/production-readiness/02-p1-production-hardening.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#polymarket-settlement-transition-tests | T2 polymarket-settlement-tests | 2026-06-03 | `src/main/java/com/example/exchange/domain/service/PolymarketOrderStateMachine.java`, `src/test/java/com/example/exchange/domain/service/PolymarketOrderStateMachineTest.java`, `docs/ai/maps/polymarket-security.md`, `docs/tasks/production-readiness/02-p1-production-hardening.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#polymarket-trade-lifecycle-projection | T2 polymarket-trade-projection | 2026-06-03 | `src/main/java/com/example/exchange/domain/service/Polymarket*`, `src/test/java/com/example/exchange/domain/service/Polymarket*`, `docs/ai/maps/polymarket-security.md`, `docs/tasks/production-readiness/02-p1-production-hardening.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#polymarket-integration-state-matrix | T2 polymarket-state-matrix | 2026-06-03 | `src/main/java/com/example/exchange/domain/service/Polymarket*`, `src/test/java/com/example/exchange/domain/service/Polymarket*`, `docs/ai/maps/polymarket-security.md`, `docs/tasks/production-readiness/02-p1-production-hardening.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#polymarket-integration | T1 polymarket-schema-versioning | 2026-06-03 | `src/main/java/com/example/exchange/domain/model/dto`, `src/main/java/com/example/exchange/domain/service/Polymarket*`, `src/test/java/com/example/exchange/domain/service/Polymarket*`, `docs/ai/maps/polymarket-security.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#observability-matching-metrics | T2 observability-matching-metrics | 2026-06-03 | `src/main/java/com/example/exchange/application/service/OperationalMetricsService.java`, `src/main/java/com/example/exchange/domain/model/dto/OperationalMetricsSnapshot.java`, `src/test/java/com/example/exchange/application/service/OperationalMetricsServiceTest.java`, `docs/en/observability.md`, `docs/zh-TW/observability.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#market-data-gateway-scaling-notes | T1 market-data-gateway | 2026-06-03 | `docs/en/market-data-gateway-scaling.md`, `docs/zh-TW/market-data-gateway-scaling.md`, `docs/en/todo.md`, `docs/zh-TW/todo.md`, `docs/en/current-state.md`, `docs/zh-TW/current-state.md`, `docs/tasks/production-readiness/02-p1-production-hardening.md`, `docs/ai/maps/reliability-market-data.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#market-maker-quote-api-frequency-limit-policy | T1 market-maker-api | 2026-06-03 | `src/main/java/com/example/exchange/interfaces/web/controller/MarketMakerController.java`, `src/main/java/com/example/exchange/interfaces/web/security`, `src/main/java/com/example/exchange/infra/config`, `src/test/java/com/example/exchange/interfaces/web/security`, `docs/ai/maps/market-maker-hedging.md`, `docs/tasks/production-readiness/02-p1-production-hardening.md`, `docs/en/todo.md`, `docs/zh-TW/todo.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#market-maker-hedge-execution-api-frequency-limit-policy | T1 market-maker-api | 2026-06-03 | `src/main/java/com/example/exchange/interfaces/web/controller/MarketMakerController.java`, `src/main/java/com/example/exchange/interfaces/web/security`, `src/main/java/com/example/exchange/infra/config/MarketMakerApiProperties.java`, `src/test/java/com/example/exchange/interfaces/web/security`, `docs/ai/maps/market-maker-hedging.md`, `docs/tasks/production-readiness/02-p1-production-hardening.md`, `docs/en/todo.md`, `docs/zh-TW/todo.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#market-maker-endpoint-audit-fields | T1 market-maker-api | 2026-06-03 | `src/main/java/com/example/exchange/interfaces/web/controller/MarketMakerController.java`, `src/main/java/com/example/exchange/interfaces/web/security`, `src/test/java/com/example/exchange/interfaces/web`, `docs/ai/maps/market-maker-hedging.md`, `docs/tasks/production-readiness/02-p1-production-hardening.md`, `docs/en/todo.md`, `docs/zh-TW/todo.md` |  |
| done | docs/tasks/production-readiness/02-p1-production-hardening.md#polymarket-transition-matrix | T1 polymarket-transition-matrix | 2026-06-03 | `docs/en/polymarket-order-transition-matrix.md`, `docs/zh-TW/polymarket-order-transition-matrix.md`, `docs/ai/maps/polymarket-security.md`, `docs/tasks/production-readiness/02-p1-production-hardening.md`, `docs/en/todo.md`, `docs/zh-TW/todo.md`, `docs/en/current-state.md`, `docs/zh-TW/current-state.md` |  |
| done | docs/tasks/production-readiness/03-p2-evolution.md#all-p2-task-specs | T3 p2-evolution-specs | 2026-06-03 | `docs/tasks/production-readiness/03-p2-evolution.md`, `docs/tasks/p2`, `docs/tasks/README.md`, `docs/tasks/production-readiness/README.md`, `docs/en/todo.md`, `docs/zh-TW/todo.md`, `docs/en/current-state.md`, `docs/zh-TW/current-state.md` |  |

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
