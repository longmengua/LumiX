# LumiX 文件總覽

這是 LumiX 的正式營運級文件集。目標是讓審核者、架構師、AI agent、mini 工程師都能用最少上下文理解系統並安全施工。

## 文件閱讀路徑

```text
Human reviewer
  |
  +-- docs/OPERATING_EXCHANGE_MASTER_PLAN.md
  +-- docs/ARCHITECTURE_TEXT_MAP.md
  +-- docs/PRODUCTION_READINESS_GATES.md
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
  +-- docs/ai/AI_CONTEXT_ROUTING.md
  +-- docs/phases/PHASE_12_DATABASE_SCHEMA/README.md
```

## 章節

```text
docs/
  ai/              AI agent 工作規則與 token 節省策略
  architecture/    系統架構、容器、元件、部署、流程圖
  product/         商業模型、使用者流程、費率與營運產品範圍
  frontend/        前端頁面與狀態管理規格
  backend/         Java 後端模組、交易邊界、API 與錯誤政策
  exchange-core/   帳本、凍結、撮合、結算、錢包、風控、對帳
  engineering/     程式碼註解與工程規範
  operations/      部署、監控、事故、資安與上線檢查
  phases/          Phase 12 到 Phase 36 的施工規格
  adr/             架構決策紀錄
  templates/       任務卡、審核報告、ADR 模板
```

## 圖表規則

所有 UML / 架構圖 / ERD / 流程圖一律使用純文字圖，方便 AI、review diff、終端機與 GitHub preview 直接閱讀。
