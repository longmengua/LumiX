# AI_MODEL_GATE.md

本文件定義不同 AI agent 可以做什麼。

## Capability gate

```text
+----------------------+----------------------+----------------------+----------------------+
| Area                 | Mini engineer        | Architect agent      | Human reviewer       |
+----------------------+----------------------+----------------------+----------------------+
| Docs wording         | yes                  | yes                  | final approval       |
| Low-risk frontend    | yes                  | review boundary      | optional             |
| Schema migration     | yes, task-bounded    | design review        | required for funds   |
| Ledger invariant     | no unilateral change | propose only         | required             |
| Matching behavior    | no unilateral change | propose only         | required             |
| Settlement behavior  | no unilateral change | propose only         | required             |
| Withdrawal behavior  | no unilateral change | propose only         | required             |
| Security control     | no bypass           | propose only         | required             |
+----------------------+----------------------+----------------------+----------------------+
```

## Mini may do

- Implement one Phase 12 task at a time.
- Add migrations following the approved schema design.
- Add tests that validate schema shape and constraints.
- Update task status documents.
- Refactor local code only when needed by the task.

## Mini must not do

- Create runtime fund movement.
- Create production matching execution.
- Create withdrawal signing or broadcast logic.
- Change invariant definitions.
- Skip test or review notes.
- Use mock logic in production path.

## Architect may do

- Split documents.
- Define boundaries.
- Produce diagrams.
- Propose schema and lifecycle decisions.
- Mark decisions needing human review.

## Human reviewer owns

- Fee policy.
- Asset listing policy.
- Withdrawal risk policy.
- Launch readiness.
- Legal / compliance go/no-go.
- Any money-affecting invariant.
