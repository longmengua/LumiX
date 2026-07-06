# AI_MODEL_GATE.md

本文件定義不同 AI agent 可以做什麼。

## 能力門檻

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

## Mini 可做

- 一次只執行一個第 12 階段 task。
- Add migrations following the approved schema design.
- Add tests that validate schema shape and constraints.
- 更新 task status 文件。
- Refactor local code only when needed by the task.

## Mini 不可做

- Create runtime fund movement.
- Create production matching execution.
- Create withdrawal signing or broadcast logic.
- Change invariant definitions.
- 不要略過測試或審查備註。
- Use mock logic in production path.

## 架構師 可做

- Split documents.
- Define boundaries.
- Produce diagrams.
- Propose schema and lifecycle decisions.
- Mark decisions needing human review.

## 人工審核者負責

- Fee policy.
- Asset listing policy.
- 提款風險政策。
- Launch readiness.
- Legal / compliance go/no-go.
- Any money-affecting invariant.
