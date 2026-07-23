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
- 第 17 階段：completed，主題是 Futures Core Model。
- 第 18 階段：completed — Futures Trading Sandbox foundation；HUMAN_REVIEW_REQUIRED 已批准。
- 第 19 階段：completed — Risk Sandbox foundation；HUMAN_REVIEW_REQUIRED 已批准。
- 第 20 階段：completed — Contract Trading Integration Gate foundation；HUMAN_REVIEW_REQUIRED 已批准。
- 第 21 階段：已正式開工，但目前只批准並完成 P21-T01 文件邊界盤點；等待 implementation review，Market Data runtime 尚未開始。
- P21-T02 到 P21-T08：仍等待逐卡人類實作批准，不得施工。
- 第 22 階段 到 第 36 階段：全域規劃計畫與 phase 草案已建立；runtime 均未開始，等待各 phase 的人類審核與明確 task-card 批准。
- 不得跳階。
- 不得宣稱 production ready，除非 `docs/governance/PRODUCTION_READINESS_GATES.md` 全部通過，且有人類審核者明確簽核。

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
  +-- docs/phases/PHASE_21_MARKET_DATA/README.md
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

docs/phases/PHASE_21_MARKET_DATA/
  task status update
  task card 與 implementation notes（僅在收到人類明確開工命令後）
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

## Git staging rules

- 每次準備提交前，必須先執行 `git status --short` 確認工作目錄內容。
- 工作目錄只包含本 task 的預期變更時，統一使用 `git add .`，避免逐檔 staging 造成不必要的使用者批准。
- 若工作目錄存在使用者、其他 task 或自動產生的未相關變更，禁止使用 `git add .`；必須改用明確路徑 staging，避免把未相關內容誤提交。

## Documentation rules

所有架構圖、UML、流程圖、狀態圖、ERD 都必須用純文字圖。

後續新增或修改的文件，原則上都要以繁體中文表達為主。
必要的技術名詞可以保留英文縮寫，但應優先提供中文語意，並在第一次出現時補上中文說明。
圖內標籤、流程說明與狀態名稱也應優先中文化，避免文件主體混用英文。

文件目錄治理規則：

- `docs/` root 只保留總索引與主題資料夾，不再新增散落的大型主題文件。
- 權威規則、phase review 與 readiness gate 只放 `docs/governance/`。
- 路線圖、phase 相依與規劃性文件只放 `docs/planning/`。
- 專案總覽、詞彙表與低風險背景參考只放 `docs/reference/`。
- AI workflow、prompt、routing 與 agent 協作文件只放 `docs/ai/`。
- 架構、產品、後端、前端、營運、exchange-core 文件各自留在對應主題資料夾。
- Phase 任務、task status、implementation notes 只放 `docs/phases/`。
- 若新增文件不屬於上述分類，必須先更新對應 `README.md` 的路由說明，再新增檔案。

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

目前 Phase 21 已正式開工，但只完成 P21-T01 的文件範圍，並等待 implementation review。Market Data runtime 尚未開始；P21-T02 到 P21-T08 未獲人類逐卡實作批准前，Codex / mini 只能進行狀態同步與唯讀檢查：

```text
Phase 21 market data pipeline task-card definition and review
```

後續 Phase 21 的施工入口為 `docs/phases/PHASE_21_MARKET_DATA/README.md`。P21-T01 的盤點與不變式結論位於 `p21-t01-implementation-review.md`；下一張候選卡為 P21-T02，但開始實作前仍必須先有經審核 task card 與人類明確批准。
Phase 18 到 Phase 20 的已批准歷史分別保留在各 phase 目錄的 `phase-*-final-review.md`。
