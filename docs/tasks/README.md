# Task Directory

This directory turns roadmap items into executable task files. Use it when the user wants to choose exactly which work item Codex should start next.

## Workflow

1. User proposes a new priority or interrupt request.
2. Codex converts it into one or more task Markdown files under this directory.
3. User chooses a task file.
4. Codex reads that task file, opens the referenced AI maps, implements the task, and updates status/docs.

## Status Legend

- `todo`: not started.
- `doing`: active in the current turn.
- `blocked`: cannot proceed without external input or environment fix.
- `done`: implemented and verified as far as the environment allows.

## Current Task Groups

| Group | Directory | Purpose |
| --- | --- | --- |
| Core exchange kernel | [core-kernel](core-kernel) | Replayable matching, ADL, bonus credit, turnover, accounting book, market-maker hedging. |
| Web applications | [web](web) | Client web and admin web task breakdown. |

## How To Ask Codex

```text
把這個插單需求轉成 task md：<需求描述>
```

Then:

```text
讀一下 docs/tasks/core-kernel/01-replayable-matching-core.md，開始做。
```
