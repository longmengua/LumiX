# AI Document Split Plan

本文件說明為什麼要拆文件，以及 AI / Codex 應該怎麼讀，才能減少 token 消耗。

## Problem

若所有資訊都集中在大文件，AI 每次任務都會讀到不需要的內容：

```text
Large master doc
  |
  +-- product scope
  +-- architecture
  +-- database
  +-- ledger
  +-- matching
  +-- wallet
  +-- deployment
  +-- phase tasks
  +-- review rules
```

問題：

- token 成本高。
- mini 容易擅自擴大 scope。
- 審核者很難定位具體改動。
- 高風險交易邏輯容易被一般任務誤碰。

## Target split

```text
Small task card
  |
  +-- reads one phase README
  +-- reads one domain README
  +-- reads one task file
  +-- references only needed architecture file
```

## Context routing rule

```text
Need product decision?       -> docs/product/
Need system shape?           -> docs/architecture/
Need Java backend boundary?  -> docs/backend/
Need ledger / wallet safety? -> docs/exchange-core/
Need deployment / incident?  -> docs/operations/
Need coding task?            -> docs/phases/<PHASE>/tasks/<TASK>.md
Need AI instruction?         -> AGENTS.md + AI_AGENT.md + docs/ai/
```

## File size policy

- `README.md` 應該是路由，不是百科全書。
- 單一任務卡只描述一個可完成的 change set。
- 高風險規則放在 domain invariant 文件，不散落在 phase 任務。
- Phase 文件可以引用 invariant，但不能重寫 invariant。
