# Team Management

Use this document for lane sizing, token budgets, timeboxes, and review cadence.

## Lane Sizes

| Size | Token Budget | Timebox | Shape |
| --- | ---: | ---: | --- |
| S | 10k-25k | 1-2 hours | One small behavior, one focused test, concise docs. |
| M | 25k-60k | Half day | Small feature slice across service/test/docs. |
| L | 60k-120k | One day | Cross-cutting behavior; split if practical. |
| XL | 120k+ | Multi-day | Do not start directly; create a design or task breakdown first. |

## Usage Accounting

Use the local script:

```bash
./shells/codex-usage.sh start <task-label>
./shells/codex-usage.sh end <task-label>
```

Report:

- Context token delta.
- Session total token delta.
- Input, cached input, output, and reasoning output deltas.
- 5h and weekly limit percentage-point deltas when available.

The script reads local Codex `token_count` events from `~/.codex/sessions`. It does not call OpenAI APIs and does not read auth files.

## Context Controls

- Start with the task file, active registry, and one relevant code map.
- Prefer targeted `rg` searches over broad file reads.
- Read indexes before sub-docs.
- Keep command output narrow.
- Update maps when discovery was expensive.

## Lead Checklist

Before assigning or approving a lane:

1. Is there exactly one task source of truth?
2. Are expected file areas narrow enough for parallel work?
3. Are focused tests named before coding starts?
4. Does another active lane own the same files?
5. Does this lane need a docs or code-map update?

