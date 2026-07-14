# LumiX 文件總覽

這是 LumiX 的正式營運級文件集。目標是讓審核者、架構師、AI agent、mini 工程師都能用最少上下文理解系統並安全施工。

這份 `README.md` 只負責導覽，不承載完整規格。

## 建議閱讀路徑

```text
第一次看專案
  |
  +-- docs/reference/overview.md
  +-- docs/governance/OPERATING_EXCHANGE_MASTER_PLAN.md
  +-- docs/architecture/ARCHITECTURE_TEXT_MAP.md

Human reviewer
  |
  +-- docs/governance/OPERATING_EXCHANGE_MASTER_PLAN.md
  +-- docs/architecture/ARCHITECTURE_TEXT_MAP.md
  +-- docs/governance/PRODUCTION_READINESS_GATES.md
  +-- docs/phases/README.md

Architect
  |
  +-- docs/architecture/README.md
  +-- docs/exchange-core/README.md
  +-- docs/backend/README.md
  +-- docs/operations/README.md
  +-- docs/engineering/code-commenting-standard.md

Mini engineer / Codex
  |
  +-- AGENTS.md
  +-- AI_AGENT.md
  +-- AI_PROGRESS.md
  +-- docs/ai/AI_CONTEXT_ROUTING.md
  +-- docs/phases/PHASE_17_ORDER_INTAKE/README.md
```

## 目錄地圖

```text
docs/
  README.md        文件總索引，只做路由
  governance/      權威規則、review workflow、正式上線門檻
  planning/        phase 路線圖與相依圖
  reference/       專案總覽與詞彙表
  ai/              AI agent 規則、提示詞、context routing
  architecture/    系統架構、整體文字圖、容器、元件、部署、流程圖
  product/         商業模型、使用者流程、費率與營運產品範圍
  frontend/        前端頁面與狀態管理規格
  backend/         Java 後端模組、交易邊界、API 與錯誤政策
  exchange-core/   帳本、凍結、撮合、結算、錢包、風控、對帳
  engineering/     程式碼註解與工程規範
  operations/      部署、監控、事故、資安與上線檢查
  phases/          Phase 12 到 Phase 36 的施工規格
  adr/             架構決策紀錄
  templates/       任務卡、審核報告、ADR 模板
  meta/            文件重整輔助材料，非日常施工入口
```

## 整理原則

- `docs/` root 只保留總索引與主題資料夾，不再放散落的大型規格文件。
- `README.md` 只做路由，不重寫其他文件已定義的規則。
- 權威規則放 `docs/governance/`，規劃放 `docs/planning/`，背景參考放 `docs/reference/`。
- Phase 任務與 implementation notes 只放 `docs/phases/`，避免規則散落在其他目錄。
- 新增文件時，先判斷應落在哪個主題資料夾；不要因為方便就回填到 `docs/` root。

## 圖表規則

所有 UML / 架構圖 / ERD / 流程圖一律使用純文字圖，方便 AI、review diff、終端機與 GitHub preview 直接閱讀。
