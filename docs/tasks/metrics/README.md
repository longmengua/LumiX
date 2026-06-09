# AI Team Metrics

Use this directory when the team wants repository-local tracking instead of an external PM dashboard. Keep one file per month, for example `2026-06.md`.

## Monthly File Template

```markdown
# AI Team Metrics: YYYY-MM

## Summary

| Metric | Value | Notes |
| --- | ---: | --- |
| Completed lanes |  |  |
| Median token usage: S |  |  |
| Median token usage: M |  |  |
| Median token usage: L |  |  |
| Median cycle time: S |  |  |
| Median cycle time: M |  |  |
| Median cycle time: L |  |  |
| Reopened lanes |  |  |
| Stale active claims |  |  |

## Lane Log

| Date | Lane | Owner | Size | Token Usage | Time | Tests | Result | Context Notes |
| --- | --- | --- | --- | ---: | ---: | --- | --- | --- |
| YYYY-MM-DD | `docs/tasks/...` | T1 example | M | 25k-40k | 3h | `./mvnw -Dtest=... test` | done | Used one code map. |

## Improvements

- <Repeated context waste converted into a code-map or task-template change.>
- <Task type that should be split smaller next month.>
```

## Rules

- Record ranges when exact token usage is unavailable.
- Prefer monthly summaries over per-turn bureaucracy.
- Convert repeated waste into better `docs/ai/maps/*.md` entries or narrower task files.
