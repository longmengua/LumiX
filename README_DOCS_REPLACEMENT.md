# LumiX Production Docs Replacement Pack

這包內容的目標是把 LumiX 的文件改成可以支援正式營運交易所的施工文件，而不是 MVP 筆記。

## 包含內容

- `docs/`：完整新文件目錄。
- `AGENTS.md`：Codex / agent 優先讀取的根規則。
- `AI_AGENT.md`：AI 施工規則、上下文路由、Phase 17 開工指令。
- `AI_START_HERE.md`：人與 AI 都能讀的入口。
- `AI_PROGRESS.md`：目前權威進度。
- `AI_MODEL_GATE.md`：不同 AI 能做什麼、不能做什麼。
- `AI_CONTINUE_PROMPT_V3.md`：接續施工提示詞。
- `REPLACE_DOCS.md`：替換指令。

## 設計重點

1. 文件依照 AI token 成本拆分，不讓 agent 每次讀全專案。
2. 架構文件用文字圖呈現，方便 code review 與純文字 diff。
3. Phase 17 已整理成 mini 可以接的任務入口。
4. 高風險金流邏輯有明確禁區與人審 gate。
