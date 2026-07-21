# AI 開始入口

LumiX 目前進入正式營運交易所架構化施工階段。請不要把這個專案當成 demo 或 MVP。

## 現在做到哪裡

- 第 11 階段：文件層級 production architecture reset 完成。
- 第 12 階段 到 第 16 階段：已完成。
- 第 17 階段 到第 20 階段：sandbox foundations 已完成，並已通過對應的人類審核。
- 第 21 階段：Market Data Pipeline，已規劃但尚未開工，等待人類明確命令與 task card 審核。
- 第 22 階段 到第 36 階段：規劃中，尚未開始。

## AI 先讀什麼

```text
AGENTS.md
AI_AGENT.md
AI_PROGRESS.md
docs/ai/AI_CONTEXT_ROUTING.md
docs/phases/PHASE_21_MARKET_DATA/README.md
```

## 第一個可執行任務

```text
目前沒有可直接實作的 task；Phase 21 必須先收到人類開工命令並完成 task card 審核。
```

## 不要做什麼

- 不要跳到撮合引擎。
- 不要實作真實資金扣帳。
- 不要實作真實提款。
- 不要把 stub 改名成 production。
- 不要把 mock adapter 接成正式邏輯。

如果你只是想找文件入口，先回到 `docs/README.md`；如果你是要真的開工，才從上面的 agent 路徑開始。
