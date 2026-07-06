# Mini 工作流程

## 單任務循環

```text
+------------+     +------------+     +------------+     +------------+
| Read task  | --> | Implement  | --> | Verify     | --> | Summarize  |
+------------+     +------------+     +------------+     +------------+
       ^                                                     |
       |                                                     v
       +-------------------- next task after review ----------+
```

## 不要多工

Mini should not combine P12-T01 and P12-T02 in one change unless reviewer explicitly asks.

## 必要結尾說明

```text
Task:
Status:
Files changed:
Verification:
Risk:
Human review needed:
Next task:
```
