<!-- File purpose: bilingual repository documentation catalog grouped by product docs, technical docs, TODO docs, AI docs, and task docs. -->
# Java21 Match Hub Documentation

| Language | Project Summary |
| --- | --- |
| English | Java21 Match Hub is a Java 21 + Spring Boot backend for an exchange core and Polymarket integration. |
| 繁體中文 | Java21 Match Hub 是 Java 21 + Spring Boot 後端，涵蓋交易所核心與 Polymarket 整合。 |

## Documentation Categories

| Category | English Description | 中文說明 | English | 繁體中文 |
| --- | --- | --- | --- | --- |
| Product Documentation | Business-facing overview: product scope, business features, business modules, APIs, and the order placement journey. | 面向產品與業務的總覽：產品範圍、業務功能、業務模塊、API 與下單鏈路。 | [docs/en/README.md](docs/en/README.md) | [docs/zh-TW/README.md](docs/zh-TW/README.md) |
| Current State | Quick status dashboard: completion level, MVP baseline, production blockers, and near-term priorities. | 目前狀態儀表板：完成度、MVP baseline、production blocker 與近期優先順序。 | [docs/en/current-state.md](docs/en/current-state.md) | [docs/zh-TW/current-state.md](docs/zh-TW/current-state.md) |
| Technical Documentation | Engineering-facing notes: architecture review, module structure, API curl scripts, matching engine notes, local infrastructure, and implementation details. | 面向工程的說明：架構分析、模塊結構、API curl 腳本、撮合引擎說明、本機基礎設施與實作細節。 | [docs/en/technical.md](docs/en/technical.md) | [docs/zh-TW/technical.md](docs/zh-TW/technical.md) |
| TODO Documentation | Production-readiness roadmap: required work before real funds or production traffic, grouped by priority and domain. | 正式環境待辦清單：真實資金或正式流量前需要補齊的工作，依優先級與領域分類。 | [docs/en/todo.md](docs/en/todo.md) | [docs/zh-TW/todo.md](docs/zh-TW/todo.md) |
| AI Documentation | Compact startup guide and code map for coding agents. | 給 Codex/代理使用的精簡開工入口與程式碼地圖。 | [docs/en/ai.md](docs/en/ai.md) | [docs/zh-TW/ai.md](docs/zh-TW/ai.md) |
| Task Documentation | Selectable task files for roadmap and interrupt work. | roadmap 與插單工作的可點名 task md。 | [docs/en/tasks.md](docs/en/tasks.md) | [docs/zh-TW/tasks.md](docs/zh-TW/tasks.md) |

## Local Startup

| Language | Description |
| --- | --- |
| English | Start local infrastructure, then run the Spring Boot application. |
| 繁體中文 | 先啟動本機基礎設施，再啟動 Spring Boot 應用程式。 |

```bash
docker compose up -d
./mvnw spring-boot:run
```

| Language | API Endpoint |
| --- | --- |
| English | The default API endpoint is `http://localhost:8080`. |
| 繁體中文 | 預設 API endpoint 是 `http://localhost:8080`。 |

## AI Usage

For parallel AI work, read `AGENTS.md`, `docs/ai/team-collaboration.md`, and `docs/tasks/active.md` first, then claim one task in `docs/tasks/active.md`, commit and push that claim before implementation.

When asking an AI how to start work or how to open multiple terminals, ask it to read this `README.md` first and return copy-paste-ready prompts for each terminal. For parallel work, those prompts must include a separate `git worktree` path, separate branch, task lane, claim/push requirement, and an explicit instruction not to write code in the current worktree.

| Language | How To Use |
| --- | --- |
| English | Use these prompts when asking Codex to work from the maintained documentation. |
| 繁體中文 | 跟 Codex 協作時，可以直接使用下列表格中的指令。 |

### English Prompts

| Purpose | Prompt |
| --- | --- |
| Ask what can be started next without changing code | `Read docs/en/tasks.md and tell me which tasks can be started first. Do not change code yet.` |
| Start the first core-kernel task | `Read docs/tasks/core-kernel/01-replayable-matching-core.md and start implementation.` |
| Convert an interrupt request into task files | `Convert this interrupt request into task md: build the market-maker hedging work first.` |
| Continue production TODO from the AI docs | `Read AGENTS.md and docs/en/ai.md, then continue the production TODO.` |

### 繁體中文指令

| 用途 | 指令 |
| --- | --- |
| 先看有哪些任務可做，不改 code | `讀一下 docs/zh-TW/tasks.md，告訴我目前可以先做哪些 task，先不要改 code。` |
| 開始第一個核心內核任務 | `讀一下 docs/tasks/core-kernel/01-replayable-matching-core.md，開始做。` |
| 把插單需求轉成任務檔 | `把這個插單需求轉成 task md：先做做市商對沖。` |
| 依 AI 文件繼續 production TODO | `讀一下 AGENTS.md 和 docs/zh-TW/ai.md，然後繼續 production TODO。` |

### Context Summary

| Language | Description |
| --- | --- |
| English | Run this command for a quick machine-readable context summary before broad exploration. |
| 繁體中文 | 在大範圍探索前，可先用這個指令產生 Codex 開工摘要。 |

```bash
./shells/ai-context.sh
```

## Direct Links

| Category | English | 繁體中文 |
| --- | --- | --- |
| AI Documentation | [docs/en/ai.md](docs/en/ai.md) | [docs/zh-TW/ai.md](docs/zh-TW/ai.md) |
| Task Documentation | [docs/en/tasks.md](docs/en/tasks.md) | [docs/zh-TW/tasks.md](docs/zh-TW/tasks.md) |
| Agent Startup Guide | [AGENTS.md](AGENTS.md) | [AGENTS.md](AGENTS.md) |
