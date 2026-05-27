<!-- 檔案用途：中文文件目錄，按產品文件、技術文件、待辦清單文件分類。 -->
# Java21 Match Hub 文件目錄

根目錄 [README.md](README.md) 是多國語言文件總入口。這份中文入口按三大類整理文件。

| 分類 | 中文說明 | English Description | 繁體中文 | English |
| --- | --- | --- | --- | --- |
| 產品文件 | 面向產品與業務的總覽：產品範圍、業務功能、業務模塊、API 與下單鏈路。 | Business-facing overview: product scope, business features, business modules, APIs, and the order placement journey. | [docs/zh-TW/README.md](docs/zh-TW/README.md) | [docs/en/README.md](docs/en/README.md) |
| 技術文件 | 面向工程的說明：架構分析、模塊結構、API curl 腳本、撮合引擎說明、本機基礎設施與實作細節。 | Engineering-facing notes: architecture review, module structure, API curl scripts, matching engine notes, local infrastructure, and implementation details. | [docs/zh-TW/technical.md](docs/zh-TW/technical.md) | [docs/en/technical.md](docs/en/technical.md) |
| 待辦清單文件 | 正式環境待辦清單：真實資金或正式流量前需要補齊的工作，依優先級與領域分類。 | Production-readiness roadmap: required work before real funds or production traffic, grouped by priority and domain. | [docs/zh-TW/todo.md](docs/zh-TW/todo.md) | [docs/en/todo.md](docs/en/todo.md) |
| AI 文件 | 給 Codex/代理使用的精簡開工入口與程式碼地圖。 | Compact startup guide and code map for coding agents. | [docs/zh-TW/ai.md](docs/zh-TW/ai.md) | [docs/en/ai.md](docs/en/ai.md) |
| 任務文件 | roadmap 與插單工作的可點名 task md。 | Selectable task files for roadmap and interrupt work. | [docs/zh-TW/tasks.md](docs/zh-TW/tasks.md) | [docs/en/tasks.md](docs/en/tasks.md) |

## AI 使用指引

跟 Codex 協作時可以直接用這些指令：

```text
讀一下 docs/zh-TW/tasks.md，告訴我目前可以先做哪些 task，先不要改 code。
讀一下 docs/tasks/core-kernel/01-replayable-matching-core.md，開始做。
把這個插單需求轉成 task md：先做做市商對沖。
讀一下 AGENTS.md 和 docs/zh-TW/ai.md，然後繼續 production TODO。
```

快速產生 Codex 開工摘要：

```bash
./shells/ai-context.sh
```

AI 文件：[docs/zh-TW/ai.md](docs/zh-TW/ai.md) / [docs/en/ai.md](docs/en/ai.md)

任務文件：[docs/zh-TW/tasks.md](docs/zh-TW/tasks.md) / [docs/en/tasks.md](docs/en/tasks.md)
