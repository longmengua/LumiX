# AI Handoffs

Use this directory only when an agent must stop with useful unfinished work. Completed tasks do not need handoff notes; the final response and git history are enough.

## File Naming

Use a short dated name:

```text
YYYY-MM-DD-area-summary.md
```

Example:

```text
2026-06-01-market-data-gateway.md
```

## Template

```markdown
# <Task Title>

Status: doing | blocked
Owner: <human or agent label if known>
Started: YYYY-MM-DD

## Goal

<One sentence describing the intended outcome.>

## Read First

- <task file or TODO section>
- <relevant docs/ai/maps/*.md>

## Current State

- <what has already changed or been discovered>

## Next Step

- <the next concrete implementation or verification step>

## Tests

- <tests already run and result>
- <tests still needed>

## Blockers

- <external dependency, decision, or environment problem; write "None" if clear>
```

## Cleanup

After the task is completed and documented, either delete the handoff note in the same change set or update its status to `done` if the history is useful.
