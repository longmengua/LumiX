# Task Directory

This directory turns roadmap items into executable task files. Use it when the user wants to choose exactly which work item Codex should start next.

## Workflow

1. User proposes a new priority or interrupt request.
2. Codex converts it into one or more task Markdown files under this directory.
3. User chooses a task file.
4. Codex checks [active.md](active.md) for active parallel work.
5. Codex claims the selected task in [active.md](active.md), including size, token budget, ETA, expected files, and focused tests when useful.
6. Codex commits and pushes that claim, then starts implementation.
7. Codex reads that task file, opens the referenced AI maps, implements the task, and updates status/docs.

## Parallel Terminal Pattern

When opening several Codex terminals, use one terminal per lane:

```text
T1 matching-command-log
T2 polymarket-clob-state
T3 abuse-controls
T4 docs-task-splitter
```

Each lane should claim exactly one task file or code-map area in [active.md](active.md), with expected files listed before coding starts. Do not start implementation until the claim commit has been pushed.

Use [../ai/team-management.md](../ai/team-management.md) for lane sizing, token budget ranges, timeboxes, and metric tracking.

## Status Legend

- `todo`: not started.
- `doing`: active in the current turn.
- `blocked`: cannot proceed without external input or environment fix.
- `done`: implemented and verified as far as the environment allows.

## Task Sizing

Use these defaults in task files and claim rows:

| Size | Token Budget | Timebox | Use For |
| --- | ---: | ---: | --- |
| S | 10k-25k | 1-2 hours | One small behavior or doc/code-map update. |
| M | 25k-60k | Half day | Normal feature slice with focused tests. |
| L | 60k-120k | One day | Cross-cutting slice; split if practical. |

Tasks above L should be split before implementation.

## Current Task Groups

| Group | Directory | Purpose |
| --- | --- | --- |
| Active AI work | [active.md](active.md) | Shared registry for claimed, blocked, and handoff work before implementation starts. |
| Core exchange kernel | [core-kernel](core-kernel) | Replayable matching, ADL, bonus credit, turnover, accounting book, market-maker hedging. |
| Production readiness fine tasks | [production-readiness](production-readiness) | Small progress-tracking slices split from broad P0/P1/P2 TODO items. |
| Post-v1 production hardening | [post-v1](post-v1) | Transaction boundaries, production worker routing, ADL execution, market data durability, and external API idempotency. |
| P2 evolution tasks | [p2](p2) | Admin console, reporting, load testing, rollout, and compliance task specs. |
| Release freeze | [release](release) | Bounded release scope, verification checklist, and closeout runbooks. |
| Web applications | [web](web) | Client web and admin web task breakdown. |
| AI handoffs | [handoffs](handoffs) | Short notes for unfinished parallel-agent work that another agent should resume. |
| AI team metrics | [metrics](metrics) | Optional monthly token, time, velocity, and context-waste tracking. |

## How To Ask Codex

```text
把這個插單需求轉成 task md：<需求描述>
```

Then:

```text
讀一下 docs/tasks/core-kernel/01-replayable-matching-core.md，開始做。
```

For parallel work:

```text
讀一下 AGENTS.md、docs/ai/team-collaboration.md、docs/tasks/active.md。
認領 docs/tasks/core-kernel/01-replayable-matching-core.md 這條 lane。
先更新 active.md 並 commit/push claim，然後開始實作。
```
