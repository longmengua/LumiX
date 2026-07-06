# AI Agent Operating Manual

本文件給 Codex、mini、review agent 與架構 agent 使用。

## Roles

```text
+------------------+       +------------------+       +------------------+
| Human reviewer   | ----> | Architect agent  | ----> | Mini engineer    |
| final authority  |       | design boundary  |       | task execution   |
+------------------+       +------------------+       +------------------+
          ^                         |                         |
          |                         v                         v
          |               +------------------+       +------------------+
          +---------------| Review agent     | <-----| Code / docs diff |
                          | gate checking    |       | implementation   |
                          +------------------+       +------------------+
```

## Agent responsibilities

### Architect agent

- 拆分文件。
- 定義領域邊界。
- 審核高風險流程。
- 製作純文字 UML / 架構圖。
- 給 mini 可執行任務卡。

### Mini engineer

- 只執行單一任務卡。
- 不重設架構。
- 不擴大 scope。
- 遇到帳本、錢包、撮合、結算、資金安全分歧時停止並回報。

### Review agent

- 檢查是否跳 Phase。
- 檢查是否把 stub 當完成。
- 檢查是否違反資金安全不變式。
- 檢查是否缺測試或驗證方式。

## Token-saving protocol

每次任務只載入四層文件。

```text
Layer 0: AGENTS.md / AI_AGENT.md
Layer 1: docs/ai/AI_CONTEXT_ROUTING.md
Layer 2: domain README, e.g. docs/exchange-core/README.md
Layer 3: phase task card, e.g. docs/phases/PHASE_12_DATABASE_SCHEMA/tasks/P12-T01.md
```

禁止在沒有必要時讀取：

```text
all docs files
all phase files
all product docs
all architecture docs
```

## Current work instruction for Codex

開始 Phase 12 時，請照順序執行：

```text
1. Read AGENTS.md
2. Read AI_PROGRESS.md
3. Read docs/ai/AI_CONTEXT_ROUTING.md
4. Read docs/phases/PHASE_12_DATABASE_SCHEMA/README.md
5. Pick the first unchecked task under docs/phases/PHASE_12_DATABASE_SCHEMA/tasks/
6. Implement only that task
7. Run the narrowest useful tests
8. Update the task file status and notes
9. Stop and produce a review summary
```

## Human-review triggers

下列任一條件發生時，必須標記 `HUMAN_REVIEW_REQUIRED`：

```text
ledger mutation
balance projection mutation
reservation hold/release/capture rules
order matching rules
settlement rules
fee rounding rules
withdrawal approval / signing / broadcast
deposit confirmation crediting
admin balance adjustment
risk-control bypass
authentication or authorization change
secret management change
```

## Output contract for mini

完成任務後輸出：

```text
Task: P12-Txx
Files changed:
  - path
What changed:
  - concise summary
Verification:
  - command or manual check
Risk:
  - none / low / HUMAN_REVIEW_REQUIRED
Next suggested task:
  - P12-Tyy
```
