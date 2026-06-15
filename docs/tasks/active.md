# Active AI Work Registry

This file is the shared starting point for parallel AI work. Update it before starting implementation work, commit it, and push it so other agents can see the active lane before they choose a task.

Keep entries short. Long notes belong in `docs/tasks/handoffs/`. For management metrics and token-budget guidance, use `docs/ai/team-management.md`.

## Protocol

1. Pull the latest branch state and merge or rebase the latest `main` into the work branch before selecting work.
2. Run `./shells/ai-context.sh` and `git status --short`.
3. Read this registry before selecting work.
4. Add or update one row with `doing`, expected file areas, and a timestamp.
   For non-trivial work, include compact metadata in the row: `size=S|M|L`, `budget=<token range>`, `eta=<timebox>`, `risk=low|medium|high`, and focused tests.
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

Replace the `_none_` row with real claim rows when work starts.

## Claim Row Template

Use one row per terminal agent. Keep `Expected Areas` narrow enough that another agent can safely choose unrelated work.

```markdown
| doing | docs/tasks/core-kernel/01-replayable-matching-core.md size=M budget=25k-60k eta=0.5d risk=medium | T1 matching-command-log | 2026-06-02 | `src/main/java/com/example/exchange/infra/matching`, `src/test/java/com/example/exchange/infra/matching`, `docs/ai/maps/order-matching.md` | tests=`./mvnw -Dtest=InMemoryMatchingEngineTest test` |
```

Terminal labels should be stable for the session, for example `T1 matching-command-log` or `T2 polymarket-clob-state`.

## Conflict Notes

If a new lane needs files already listed by another `doing` row:

1. Do not start implementation.
2. Split the work into a smaller task file or define a dependency order.
3. If useful discovery already happened, write a handoff note under `docs/tasks/handoffs/`.
