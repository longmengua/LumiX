# Tasks

This directory turns roadmap items into executable task files.

## Task File Shape

```markdown
# Task: <Name>

Status: `todo`
Size: S | M | L
Token Budget: <range>
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
- <one or two relevant docs>
```

## Workflow

1. Choose one task.
2. Check `doc/tasks/active.md`.
3. Claim the task.
4. Run `./shells/codex-usage.sh start <task-label>`.
5. Implement and verify.
6. Run `./shells/codex-usage.sh end <task-label>`.
7. Mark task done and commit.

