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
| doing | Market-maker quote reconciliation repair job | Codex-MM-Repair-02 | 2026-06-01 | `application/service`, `application/scheduler`, `interfaces/web`, market-maker tests/docs | None |
