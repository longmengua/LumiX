# AI Team Management Playbook

Use this playbook to manage a Codex-heavy team with measurable work units, predictable context usage, and reviewable delivery. It complements [team-collaboration.md](team-collaboration.md), which covers worktree isolation and conflict control.

## Management Goals

- Make every active lane visible: owner, task source, expected files, status, risk, and tests.
- Keep task slices small enough that one agent can complete them with focused context.
- Track token usage and elapsed time as delivery cost, not as vague chat history.
- Preserve enough code-map documentation that agents do not rediscover the repository on every task.
- Keep shared coordination files short; long findings belong in task files, code maps, or handoff notes.

## Operating Cadence

| Cadence | Owner | Output |
| --- | --- | --- |
| Start of day | Tech lead or coordinator | Pick lanes from `docs/tasks/`, confirm `docs/tasks/active.md` has no stale claims, assign terminal labels. |
| Before coding | Each agent | Claim row in `active.md`, pushed and visible on `main`; selected code map read; expected files and tests stated. |
| During work | Each agent | Focused commits or final patch, no broad exploration after scope is known. |
| End of lane | Each agent | Tests, docs/map updates, token usage estimate, active row marked `done` or removed after merge. |
| Weekly | Tech lead | Review velocity, token cost by lane type, recurring blockers, oversized tasks, and docs that caused rediscovery. |

## Lane Sizing

Keep one lane to one implementable behavior. Split the task before coding if more than two of these are true:

- It touches more than one code-map area.
- It needs both schema migration and API/UI work.
- It requires more than one new service or scheduler.
- It needs more than three focused test classes.
- It cannot be explained with one goal sentence and five acceptance criteria.
- Estimated token use is above 60k or estimated wall time is above one workday.

Suggested lane sizes:

| Size | Token Budget | Wall Time | Expected Shape |
| --- | ---: | ---: | --- |
| S | 10k-25k | 1-2 hours | One file group, one focused test, docs update if needed. |
| M | 25k-60k | Half day | Small feature slice across service/test/docs. |
| L | 60k-120k | One day | Cross-cutting behavior; should usually be split into two M lanes. |
| XL | 120k+ | Multi-day | Do not start directly; create design or task breakdown first. |

## Required Task File Shape

Every new task file should stay close to this structure:

```markdown
# Task: <Name>

Status: `todo`
Size: S | M | L
Token Budget: <range, for example 25k-60k>
Timebox: <hours or days>

## Goal

<One sentence.>

## Scope

- <In scope>
- <Out of scope if important>

## First Implementation Slice

1. <First narrow step>
2. <Next step>

## Acceptance Criteria

- <Observable result>
- <Focused tests or verification>

## Read First

- <one task or roadmap file>
- <one or two docs/ai/maps/*.md files>
```

## Active Registry Standard

Use `docs/tasks/active.md` as the source of truth for active work. Keep the current table format for compatibility, and add these details inside the `Task / Lane` or `Handoff` cell when useful:

- `size=S|M|L`
- `budget=25k-60k`
- `eta=4h`
- `risk=low|medium|high`
- `tests=<focused test class or command>`

Example:

```markdown
| doing | docs/tasks/p2/04-admin-reconciliation-report-screen.md size=M budget=25k-60k eta=0.5d | T4 admin-recon-report | 2026-06-10 | `src/main/resources/static`, `src/main/java/com/example/exchange/interfaces/web/controller`, `src/test/java/com/example/exchange/interfaces/web/controller` | tests=`./mvnw -Dtest=AdminReconciliationReportControllerTest test` |
```

## Token Accounting

Codex may not expose exact token usage in every interface. Use exact values when available; otherwise report a conservative range.

Track these numbers in each final lane report:

| Field | Meaning |
| --- | --- |
| Prompt tokens | User request, loaded docs, file reads, and tool outputs. Estimate when exact unavailable. |
| Completion tokens | Agent reasoning and final response. Estimate when exact unavailable. |
| Total tokens | Exact total or conservative range. |
| Context waste notes | Any large file or command output that should be avoided next time. |
| Reuse asset | Code map, task file, or command that reduced exploration. |

Final report template:

```text
Fine task: <lane>
Commit: <hash or uncommitted>
Tests: <commands and result>
Token usage: exact unavailable; estimated 25k-40k
Time: estimated 3h
Context notes: read <map>; avoided broad package scan
```

## Token Budget Controls

Use these defaults unless a task explicitly needs more:

- Start with `./shells/ai-context.sh`, `docs/tasks/active.md`, one task file, and one relevant code map.
- Prefer `rg` targeted searches over reading whole directories.
- Read Markdown indexes first; open sub-docs only when the current lane needs them.
- Cap initial file reads to 200-260 lines unless editing the file requires full context.
- Use focused tests first; run `./mvnw test` only before broad merges or release checks.
- Keep command output narrow with specific test selectors and `rg` patterns.
- Update code maps when discovery was expensive so the next agent can skip it.

Avoid:

- Reading all docs before selecting a lane.
- Asking multiple agents to analyze the same broad roadmap item.
- Dumping full test logs into chat when the failing assertion is enough.
- Starting implementation from `docs/roadmap/todo.md` when a narrower `docs/tasks/...` file exists.

## Code Graph And Context Map

The repository already uses `docs/ai/code-map.md` plus sub-maps. Treat this as the human-maintained code graph.

For each map, keep these sections concise:

- Entry points: controllers, consumers, schedulers, use cases.
- Core services and domain contracts.
- Persistence or external adapters.
- Important tests.
- Common change paths.
- Current hazards or ownership notes.

When a lane changes a core flow:

1. Update only the relevant sub-map under `docs/ai/maps/`.
2. Add new files to the map only if future agents should know them.
3. Link longer explanations to product or technical docs instead of expanding the map.
4. If a map grows too long, split it by lane type and update `docs/ai/code-map.md`.

## Folder Classification

Use the repository folders this way:

| Folder | Role | Rule |
| --- | --- | --- |
| `docs/ai/` | Agent operating docs and code graph | Keep compact and task-routing oriented. |
| `docs/ai/maps/` | Area-specific code maps | Update when ownership or core flow changes. |
| `docs/tasks/` | Executable work items | One file per implementable lane or coordinated milestone. |
| `docs/tasks/active.md` | Current claims | Short rows only; no long analysis. |
| `docs/tasks/handoffs/` | Unfinished work notes | Use only when work stops before completion. |
| `docs/overview.md` | Product overview and local startup entry point | Update when product scope or entry-point guidance changes. |
| `docs/status/` | Current state and production blockers | Update when delivered behavior changes the status baseline. |
| `docs/roadmap/` | Roadmap, TODOs, release checklist, and planning notes | Update when roadmap status or task routing changes. |
| `shells/` | Repeatable local commands and curl coverage | Add scripts when manual verification would otherwise be copied around. |
| `src/main/...` | Production code | Follow architecture dependency direction from `AGENTS.md`. |
| `src/test/...` | Focused verification | Tests should document scenario intent through names or comments. |

## Metrics Dashboard

Maintain a lightweight weekly summary in the team tool of choice, or add `docs/tasks/metrics/YYYY-MM.md` from the template in `docs/tasks/metrics/README.md` if repository-local tracking is preferred. Track:

| Metric | Target |
| --- | --- |
| Completed lanes per week | Trend, not fixed quota. |
| Median token usage per S/M/L lane | Should stabilize after maps improve. |
| Median cycle time per S/M/L lane | Flags oversized tasks early. |
| Rework rate | Lanes reopened after completion. |
| Test failure escape rate | Failures found after lane final report. |
| Stale active claims | Should be zero at end of day. |
| Context waste notes | Convert repeated waste into map or task-template improvements. |

## Lead Review Checklist

Before assigning or approving a lane:

1. Is there exactly one task source of truth?
2. Is the expected file area narrow enough for parallel work?
3. Does the lane have a token budget and timebox?
4. Are focused tests named before coding starts?
5. Does another active lane own the same files?
6. Does this lane need a code-map update after completion?

If any answer is unclear, split the task or add a short clarification to the task file before implementation.
