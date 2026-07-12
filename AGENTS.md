# AGENTS.md

本檔是 LumiX repo 的 AI agent 入口。Codex、mini 或任何自動化 coding agent 在動手前都必須先讀完本檔。

## Project truth

LumiX 不是 MVP。LumiX 的目標是可以正式營運、可以承載真實資金、可以審計、可以維運、可以逐步商業化的交易所系統。

目前權威狀態：

- 第 11 階段：已完成文件層級的 production architecture reset。
- 第 12 階段：已完成 Production Database Schema 與 Migration foundation。
- 第 13 階段：已完成 backend module foundation 與 API boundary。
- 第 14 階段：已完成 immutable ledger engine foundation。
- 第 15 階段：已完成 trading runtime core foundation。
- 第 16 階段：已完成 spot sandbox foundation。
- 第 17 階段：下一個實作階段，主題是 Futures Core Model。
- 第 18 階段 到 第 36 階段：已規劃但未開始。
- 不得跳階。
- 不得宣稱 production ready，除非 `docs/PRODUCTION_READINESS_GATES.md` 全部通過，且有人類審核者明確簽核。

## Mandatory reading order

每次任務開始，只讀必要上下文。

```text
Root agent entry
  |
  +-- AGENTS.md
  +-- AI_AGENT.md
  +-- AI_PROGRESS.md
  |
  +-- docs/ai/AI_CONTEXT_ROUTING.md
  |
  +-- docs/phases/README.md
  +-- docs/phases/PHASE_17_ORDER_INTAKE/README.md
  |
  +-- task-specific file listed by the phase README
```

不要一開始讀完整 `docs/`。除非任務明確要求架構審核，否則過量讀文件會造成 token 浪費與錯誤推論。

## 目前允許施工範圍

目前允許施工範圍：

```text
server/
  current phase implementation
  schema / contract / runtime tests
  read-only technical documentation updates

docs/phases/PHASE_17_ORDER_INTAKE/
  task status update
  implementation notes
```

目前不允許施工範圍：

```text
production ledger mutation
real fund transfer
real wallet deposit
real wallet withdrawal
matching engine execution
settlement engine mutation
fee collection runtime
admin manual balance adjustment
KYC / AML bypass
security bypass
```

## Engineering rules

- Java 後端遵守 Java 21 + Spring Boot 3 的專案方向。
- 前端遵守 React + TypeScript + Vite 的專案方向。
- 金額、數量、價格、費用不得使用 binary floating point。
- 資產數值應使用整數最小單位或可控精度 decimal。
- 交易所核心資料必須可追蹤、可重放、可審計。
- 帳本必須 immutable append-only；修正只能追加 reversal / adjustment entry。
- API 需要 idempotency 設計，尤其是下單、取消、提款、入金確認。
- 不允許用 TODO / placeholder 偽裝完成。
- 不允許把 mock adapter 接到 production path。
- 所有新增或修改的程式碼都必須具備足夠註解，遵守 `docs/engineering/code-commenting-standard.md`。
- 註解必須使用繁體中文，優先說明為什麼這樣做，而不是重複程式碼表面行為。

## Documentation rules

所有架構圖、UML、流程圖、狀態圖、ERD 都必須用純文字圖。

允許：

```text
+---------+       +---------+
| Client  | ----> | Server  |
+---------+       +---------+
```

禁止：

```text
Mermaid
PlantUML
圖片檔
外部線上圖表工具
```

## Definition of done for any task

每個任務至少要有：

1. Scope 明確。
2. 實作或文件變更只碰任務範圍。
3. 測試或驗證方法清楚。
4. 若改到 schema，要有 migration 順序與 rollback 說明。
5. 若改到交易、帳本、錢包、風控，要在 PR 說明標記 `HUMAN_REVIEW_REQUIRED`。
6. 更新對應階段文件的 task status。

## Start here for current task

目前建議 Codex / mini 優先處理：

```text
Phase 17 README and task split
Phase 17 implementation notes
```

第一張任務卡請從 `docs/phases/PHASE_17_ORDER_INTAKE/README.md` 開始。
