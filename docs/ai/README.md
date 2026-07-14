# AI 文件

AI 文件的目標是讓 agent 少讀、準確讀、安全做，同時讓人類可以快速看懂目前的 agent workflow。

## 穩定入口

```text
AGENTS.md
  |
AI_AGENT.md
  |
AI_PROGRESS.md
  |
docs/ai/AI_CONTEXT_ROUTING.md
  |
phase task card
```

## 這裡放什麼

```text
AI_CONTEXT_ROUTING.md    任務類型對應該讀哪些文件
AI_REVIEW_CHECKLIST.md   AI review 時的檢查清單
MINI_WORKFLOW.md         mini / coding agent 的任務節奏
AI_START_HERE.md         人類與 AI 共用的快速入口
AI_MODEL_GATE.md         不同 agent 的能力邊界
AI_CONTINUE_PROMPT_V3.md 接續施工提示詞
CODEX_PHASE_PROMPTS.md   phase 實作 / review 提示詞片段
AI_DOCUMENT_SPLIT_PLAN.md 為什麼這樣拆文件與 token 策略
```

## 閱讀建議

- 一般 AI 任務不要把整個 `docs/ai/` 全讀完；優先讀 `AI_CONTEXT_ROUTING.md`。
- 只有在調整 agent workflow、prompt、文件拆分策略時，才需要讀 `AI_DOCUMENT_SPLIT_PLAN.md` 或 `CODEX_PHASE_PROMPTS.md`。
