# AI Documentation

This directory contains compact, agent-facing context. It is intentionally split so an agent can read the index first and open only the relevant sub-map.

Start here:
- [code-map.md](code-map.md)
- [team-collaboration.md](team-collaboration.md) when multiple agents are working in parallel.
- [team-management.md](team-management.md) for token budgets, lane sizing, cadence, and team metrics.

Sub-maps live in [maps/](maps/).

## How To Ask Codex

Use a Markdown file as the task entry point:

```text
讀一下 doc/roadmap/todo.md，從最前面的未完成 P0 開始做。
```

Codex should read that file first, open the relevant map under `doc/ai/maps/`, and either start the clear next task or ask which task to start if the file leaves multiple good options.

## Code Comment Standard

AI-generated or AI-modified code should include helpful comments in both production and test code. Prefer comments that explain business intent, state transitions, replay/recovery behavior, accounting effects, risk decisions, invariants, and edge cases. Tests should make setup, action, and expected result clear through comments or `@DisplayName`.

Do not add noisy comments that only restate syntax.

For interrupt work, ask Codex to create a task file first:

```text
把這個插單需求轉成 task md：先做做市商對沖。
```

Then choose the generated file:

```text
讀一下 doc/tasks/core-kernel/05-market-maker-hedging.md，開始做。
```

## Team Management

Use [team-management.md](team-management.md) when assigning work across a Codex-heavy team. It defines lane sizes, token budget ranges, timeboxes, active-registry metadata, code-map upkeep, and weekly metrics.
