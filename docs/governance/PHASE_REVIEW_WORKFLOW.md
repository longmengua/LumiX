# 階段 Review Workflow

每個 階段 都有固定流程。

## Workflow

```text
+----------------+     +----------------+     +----------------+
| Phase scope    | --> | Task execution | --> | Local checks   |
+----------------+     +----------------+     +----------------+
                                                        |
                                                        v
+----------------+     +----------------+     +----------------+
| Human sign-off | <-- | Review report  | <-- | Risk tagging   |
+----------------+     +----------------+     +----------------+
```

## Required review fields

```text
Phase:
Task:
Scope:
Files changed:
Tests run:
Schema changed: yes/no
Money-impacting: yes/no
HUMAN_REVIEW_REQUIRED: yes/no
Rollback notes:
Next task:
```

## Automatic fail conditions

- 任務擴大 scope。
- 改到高風險邏輯但沒有標記人審。
- migration 沒有 rollback 策略。
- schema 沒有 精度 / uniqueness / index 說明。
- 把 stub / TODO / mock 寫成完成。
- 沒有測試或可驗證說明。
